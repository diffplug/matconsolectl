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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is used only from inside of the MATLAB JVM. It is responsible for creating proxies and sending them to
 * the receiver over RMI.
 * <br><br>
 * While this class is package private, it can be seen by MATLAB, which does not respect the package privateness of the
 * class. The public methods in this class can be accessed from inside the MATLAB environment.
 * 
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabConnector
{
    private static JMIWrapper _wrapper = new JMIWrapper();
    
    /**
     * Used to establish connections on a separate thread.
     */
    private static final ExecutorService _establishConnectionExecutor = Executors.newSingleThreadExecutor();
    
    /**
     * Private constructor so this class cannot be constructed.
     */
    private MatlabConnector() { }
    
    static JMIWrapper getJMIWrapper()
    {
        return _wrapper;
    }
    
    /**
     * Called from MATLAB to create a proxy. Creates the proxy and then sends it over RMI to the Java program running in
     * a separate JVM.
     * 
     * @param receiverID the key that binds the receiver in the registry
     * @param proxyID the unique identifier of the proxy being created
     */
    public static void connectFromMatlab(String receiverID, String proxyID)
    {
        connect(receiverID, proxyID, false);
    }
    
    static void connect(String receiverID, String proxyID, boolean existingSession)
    {
        //Establish the connection on a separate thread to allow MATLAB to continue to initialize
        //(If this request is coming over RMI then MATLAB has already initialized, but this will not cause an issue.)
        _establishConnectionExecutor.submit(new EstablishConnectionRunnable(receiverID, proxyID, existingSession));
    }
    
    /**
     * A runnable which sets up matlabcontrol inside MATLAB and sends over the remote JMI wrapper.
     */
    private static class EstablishConnectionRunnable implements Runnable
    {
        private final String _receiverID;
        private final String _proxyID;
        private final boolean _existingSession;
        
        private EstablishConnectionRunnable(String receiverID, String proxyID, boolean existingSession)
        {
            _receiverID = receiverID;
            _proxyID = proxyID;
            _existingSession = existingSession;
        }

        @Override
        public void run()
        {
            //If MATLAB was just launched, wait for it to initialize and make it available for reconnection
            if(!_existingSession)
            {
                //Attempt to wait for MATLAB to initialize, if not still proceed
                try
                {
                    Thread.sleep(5000L);
                }
                catch(InterruptedException ex)
                {
                    System.err.println("Unable to wait for MATLAB to initialize, problems may occur");
                    ex.printStackTrace();
                }

                //Make this session of MATLAB of visible over RMI so that reconnections can occur, proceed if it fails
                try
                {
                    MatlabBroadcaster.broadcast();
                }
                catch(MatlabConnectionException ex)
                {
                    System.err.println("Reconnecting to this session of MATLAB will not be possible");
                    ex.printStackTrace();
                }
            }

            //Send the remote JMI wrapper
            try
            {
                //Get registry
                Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);

                //Get the receiver from the registry
                JMIWrapperRemoteReceiver receiver = (JMIWrapperRemoteReceiver) registry.lookup(_receiverID);

                 //Register the receiver with the broadcaster
                MatlabBroadcaster.addReceiver(receiver);

                //Create the remote JMI wrapper and then pass it over RMI to the Java application in its own JVM
                receiver.registerControl(_proxyID, new JMIWrapperRemoteImpl(getJMIWrapper()), _existingSession);
            }
            catch(RemoteException ex)
            {
                System.err.println("Connection to Java application could not be established");
                ex.printStackTrace();
            }
            catch(NotBoundException ex)
            {
                System.err.println("Connection to Java application could not be established");
                ex.printStackTrace();
            }
            
            MatlabBroadcaster.connectionAttempted();
        }
    }
}