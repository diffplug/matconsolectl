package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses the Attach API intended for instrumenting running Java Virtual Machines to connect to a running session of
 * MATLAB that does not need to know about matlabcontrol in any capacity.
 * <br><br>
 * This code is highly experimental and could be improved.
 * 
 * @since 4.0.0-experimental
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabJVMFinder_Internal
{   
    private static final ConcurrentHashMap<String, Object> CLAIMED_VIRTUAL_MACHINES = new ConcurrentHashMap<String, Object>();
    
    /**
     * Attempts a connection. Returns {@code true} if a connection has been initiated.
     * 
     * @param receiverID
     * @return
     * @throws MatlabConnectionException 
     */
    static boolean connect(String receiverID) throws MatlabConnectionException
    {
        try
        {
            VirtualMachine matlabJVM = findAvailableMatlabJVM();

            boolean success;
            if(matlabJVM != null)
            {
                matlabJVM.loadAgent(Configuration.getSupportCodeLocation(), receiverID);
                matlabJVM.detach();
                success = true;
            }
            else
            {
                success = false;
            }
            
            return success;
        }
        catch(AgentInitializationException e)
        {
            throw new MatlabConnectionException("Unable to connect to existing never controlled MATLAB session", e);
        }
        catch(AgentLoadException e)
        {
            throw new MatlabConnectionException("Unable to connect to existing never controlled MATLAB session", e);
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Unable to connect to existing never controlled MATLAB session", e);
        }
    }
    
    /**
     * Attempts to claim the MATLAB JVM by binding its identifier in the RMI registry. Returns {@code true} if it was
     * successfully claimed, {@code false} otherwise. This is done to prevent multiple simultaneous connections via
     * VM attachment to occur. This must be done through the registry, not an instance variable, to properly support
     * multiple instances of matlabcontrol running at the same time.
     * 
     * @param matlabJVM
     * @return if successfully claimed
     */
    private static boolean claim(VirtualMachine matlabJVM)
    {
        boolean successfullyClaimed = false;
        
        if(!CLAIMED_VIRTUAL_MACHINES.containsKey(matlabJVM.id()))
        {   
            try
            {
                Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);

                //The name is what matters, but it is not legal to bind null, so bind a RemoteStub instead
                RemoteStub stub = new RemoteStub(){};
                CLAIMED_VIRTUAL_MACHINES.put(matlabJVM.id(), stub);
                registry.bind("MATLAB_JVM_" + matlabJVM.id(), UnicastRemoteObject.exportObject(stub, 0));

                successfullyClaimed = true;
            }
            //Means the MATLAB session has already been claimed
            catch(AlreadyBoundException ex) { }
            //Something went wrong in either getting the registry or communicating with it, it's possible the MATLAB
            //session is available and could be connected to in the future
            catch(RemoteException ex)
            {
                CLAIMED_VIRTUAL_MACHINES.remove(matlabJVM.id());
            }
        }
        
        return successfullyClaimed;
    }
    
    /**
     * Called from MATLAB when MATLAB's JVM is attached to.
     * 
     * @param receiverID
     * @param inst 
     */
    public static void agentmain(String receiverID, Instrumentation inst)
    {
        System.out.println("Attempting to connect to receiver: " + receiverID);
        MatlabConnector.connect(receiverID, true);
    }
    
    private static VirtualMachine findAvailableMatlabJVM()
    {
        VirtualMachine matlabJVM = null;
        
        List<VirtualMachineDescriptor> vmDescriptors = VirtualMachine.list();
        for(VirtualMachineDescriptor vmDescriptor : vmDescriptors)
        {
            //The process name is actually MATLAB, but is typically reported by the descriptor typically returns it as
            //an empty string. Because of this uncertanity, connect to it and read its properties in order to be more
            //certain that the JVM is in fact MATLAB.
            if(vmDescriptor.displayName().equals("") || vmDescriptor.displayName().equalsIgnoreCase("MATLAB"))
            {
                try
                {
                    VirtualMachine vm = VirtualMachine.attach(vmDescriptor.id());

                    //If the properties likely are those of a MATLAB session not broadcasting and it can be claimed
                    Properties properties = vm.getSystemProperties();
                    if(!isMatlabJVMBroadcasting(properties) && isMatlabJVM(properties) && claim(vm))
                    {
                        matlabJVM = vm;
                        break;
                    }
                    else
                    {
                        vm.detach();
                    }
                }
                //If no connection can be made, then it cannot - nothing to do about it
                catch(AttachNotSupportedException e) { }
                catch(IOException e) { }
            }
        }
        
        return matlabJVM;
    }
    
    /**
     * If the properties indicate that the MATLAB JVM is broadcasting over RMI. If that is the case then the virtual
     * machine should be ignored by this form of connection.
     * 
     * @param properties
     * @return 
     */
    private static boolean isMatlabJVMBroadcasting(Properties properties)
    {
        //This relies on the matlabcontrol.remote.status property being set when a session of MATLAB is available for
        //connection over RMI broadcast
        boolean broadcasting = (properties.getProperty("matlabcontrol.remote.status") != null);
        
        return broadcasting;
    }
    
    /**
     * Attempts to determine if the {@code properties} describe the JVM used by MATLAB. Not necessarily correct, but it
     * hopefully is.
     * 
     * @param properties
     * @return 
     */
    private static boolean isMatlabJVM(Properties properties)
    {
        boolean isMATLAB = false;
        
        //OS X specific property, but likely the most reliable
        String macName = properties.getProperty("com.apple.mrj.application.apple.menu.about.name");
        if(macName != null && macName.equals("MATLAB"))
        {
            isMATLAB = true;
        }
        
        String prefFactory = properties.getProperty("java.util.prefs.PreferencesFactory");
        if(prefFactory != null && prefFactory.contains("com.mathworks"))
        {
            isMATLAB = true;
        }
        
        String protocolHandler = properties.getProperty("java.protocol.handler.pkgs");
        if(protocolHandler != null && protocolHandler.contains("com.mathworks"))
        {
            isMATLAB = true;
        }
        
        return isMATLAB;
    }
}