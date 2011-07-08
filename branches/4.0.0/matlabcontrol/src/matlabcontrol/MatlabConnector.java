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
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

import matlabcontrol.internal.MatlabRMIClassLoaderSpi;

/**
 * This class is used only from inside of the MATLAB JVM. It is responsible for creating instances of
 * {@link JMIWrapperRemote} and sending them to a waiting receiver over RMI.
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
    /**
     * Used to establish connections on a separate thread.
     */
    private static final ExecutorService _connectionExecutor = Executors.newSingleThreadExecutor();
    
    /**
     * The most recently connected receiver retrieved from a Java program running outside of MATLAB.
     */
    private static final AtomicReference<RequestReceiver> _receiverRef =
            new AtomicReference<RequestReceiver>();
    
    /**
     * If a connection is currently in progress.
     */
    private static final AtomicBoolean _connectionInProgress = new AtomicBoolean(false);
    
    /**
     * Private constructor so this class cannot be constructed.
     */
    private MatlabConnector() { }
    
    /**
     * If this session of MATLAB is available to be connected to from an external Java program. It will be available
     * if it is not currently connected to and there is no connection in progress.
     * 
     * @return 
     */
    static boolean isAvailableForConnection()
    {
        boolean available;
        
        if(_connectionInProgress.get())
        {
            available = false;
        }
        else
        {
            RequestReceiver receiver = _receiverRef.get();

            boolean connected = false;
            if(receiver != null)
            {
                try
                {
                    receiver.getReceiverID();
                    connected = true;
                }
                catch(RemoteException e) { }
            }
            
            available = !connected;
        }
        
        return available;
    }
    
    /**
     * Called from MATLAB at launch. Creates the JMI wrapper and then sends it over RMI to the Java program running in a
     * separate JVM.
     * 
     * @param receiverID the key that binds the receiver in the registry
     * @param port the port the registry is running on
     * @param initializationTime
     */
    public static void connectFromMatlab(String receiverID, int port)
    {
        connect(receiverID, port, false);
    }
    
    /**
     * Retrieves the receiver and sends over the {@link JMIWrapperRemote} on a separate thread so that MATLAB can
     * continue to initialize.
     * 
     * @param receiverID
     * @param port
     * @param existingSession 
     */
    static void connect(String receiverID, int port, boolean existingSession)
    {
        _connectionInProgress.set(true);
        
        //Establish the connection on a separate thread to allow MATLAB to continue to initialize
        //(If this request is coming over RMI then MATLAB has already initialized, but this will not cause an issue.)
        _connectionExecutor.submit(new EstablishConnectionRunnable(receiverID, port, existingSession));
    }
    
    /**
     * A runnable which sets up matlabcontrol inside MATLAB and sends over the remote JMI wrapper.
     */
    private static class EstablishConnectionRunnable implements Runnable
    {
        private final String _receiverID;
        private final int _port;
        private final boolean _existingSession;
        
        /**
         * The classpath (with each classpath entry as an individual canonical path) of the most recently connected
         * receiver's JVM.
         * <br><br>
         * This variable can safely be volatile because the needed behavior is volatile read/write of the array, not
         * its entries. It is also unlikely the volatile behavior is actually necessary, but it could be if the thread
         * used by {@link MatlabConnector#_connectionExecutor} died and created a new one - this ensures visibility.
         */
        private static volatile String[] _previousRemoteClassPath = new String[0];
        
        private EstablishConnectionRunnable(String receiverID, int port, boolean existingSession)
        {
            _receiverID = receiverID;
            _port = port;
            _existingSession = existingSession;
        }

        @Override
        public void run()
        {
            //Validate matlabcontrol can be used
            try
            {
                JMIValidator.validateJMIMethods();
            }
            catch(MatlabConnectionException e)
            {
                System.err.println("matlabcontrol is not compatible with this version of MATLAB");
                e.printStackTrace();
                return;
            }
            
            //If MATLAB was just launched
            if(!_existingSession)
            {
                //Set the RMI class loader service provider
                System.setProperty("java.rmi.server.RMIClassLoaderSpi", MatlabRMIClassLoaderSpi.class.getName());
                
                //Make this session of MATLAB of visible over RMI so that reconnections can occur, proceed if it fails
                try
                {
                    MatlabBroadcaster.broadcast(_port);
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
                Registry registry = LocalHostRMIHelper.getRegistry(_port);

                //Get the receiver from the registry
                RequestReceiver receiver = (RequestReceiver) registry.lookup(_receiverID);

                 //Hold on the to receiver
                _receiverRef.set(receiver);
                
                //Load a security manager so that remote class loading can occur
                if(System.getSecurityManager() == null)
                {
                    System.setSecurityManager(new PermissiveSecurityManager());
                }
            
                //Tell the RMI class loader of the codebase where the receiver is from, this will allow MATLAB to load
                //classes defined in the remote JVM, but not in this one
                MatlabRMIClassLoaderSpi.setCodebase(receiver.getClassPathAsRMICodebase());
                
                //Tell MATLAB's class loader about the codebase where the receiver is from, if not then MATLAB's
                //environment will freak out when interacting with classes it cannot find the definition of and throw
                //exceptions with rather confusing messages
                String[] newClassPath = receiver.getClassPathAsCanonicalPaths();
                try
                {
                    JMIWrapper.invokeAndWait(new ModifyCodebaseCallable(_previousRemoteClassPath, newClassPath));
                    _previousRemoteClassPath = newClassPath;
                }
                catch(MatlabInvocationException e)
                {
                    System.err.println("Unable to update MATLAB's class loader; issues may arise interacting with " +
                            "classes not defined in MATLAB's Java Virtual Machine");
                    e.printStackTrace();
                }

                //Create the remote JMI wrapper and then pass it over RMI to the Java application in its own JVM
                receiver.receiveJMIWrapper(new JMIWrapperRemoteImpl(), _existingSession);
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
            
            _connectionInProgress.set(false);
        }
    }
    
    /**
     * Modifies MATLAB's dynamic class path. Retrieves the current dynamic class path, removes all of the entries
     * from the previously connected JVM, adds the new ones, and if the classpath is now different, sets a new dynamic
     * classpath.
     */
    private static class ModifyCodebaseCallable implements MatlabThreadCallable<Void>
    {
        private final String[] _toRemove;
        private final String[] _toAdd;
        
        public ModifyCodebaseCallable(String oldRemoteClassPath[], String[] newRemoteClassPath)
        {
            _toRemove = oldRemoteClassPath;
            _toAdd = newRemoteClassPath;
        }

        @Override
        public Void call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            //Current dynamic class path
            String[] curr = (String[]) proxy.returningFeval("javaclasspath", 1, "-dynamic")[0];
            
            //Build new dynamic class path
            List<String> newDynamic = new ArrayList<String>();
            newDynamic.addAll(Arrays.asList(curr));
            newDynamic.removeAll(Arrays.asList(_toRemove));
            newDynamic.addAll(Arrays.asList(_toAdd));
            
            //If the class path is different, set it
            if(!newDynamic.equals(Arrays.asList(curr)))
            {
                proxy.feval("javaclasspath", new Object[] { newDynamic.toArray(new String[newDynamic.size()]) });
            }
            
            return null;
        }
    }
}