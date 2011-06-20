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

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import matlabcontrol.MatlabProxyFactory.RequestCallback;
import matlabcontrol.MatlabProxyFactoryOptions.ImmutableFactoryOptions;

/**
 * Creates remote instances of {@link MatlabProxy}. Creating a proxy will either connect to an existing session of
 * MATLAB or launch a new session of MATLAB. This factory can be used to create any number of proxies.
 * 
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class RemoteMatlabProxyFactory implements ProxyFactory
{
    /**
     * The options that configure this instance of the factory.
     */
    private final ImmutableFactoryOptions _options;
    
    /**
     * Map of proxyIDs to {@link ProxyReceiver} instances.
     */
    private final ConcurrentMap<String, ProxyReceiver> _receivers = new ConcurrentHashMap<String, ProxyReceiver>();
    
    /**
     * The RMI registry used to communicate between JVMs. There is only ever one registry actually running on a given
     * machine, so multiple distinct programs making use of matlabcontrol all share the same underlying registry
     * (although the Java object will be different).
     */
    private static Registry _registry = null;
    
    /**
     * 
     * @param options 
     */
    public RemoteMatlabProxyFactory(ImmutableFactoryOptions options)
    {
        _options = options;
    }
    
    /**
     * Uses the {@link #_options} and {@code runArg} to build the arguments that launch MATLAB.
     * 
     * @param options
     * @return
     * @throws InitializationException 
     */
    private List<String> buildProcessArguments(String runArg) throws MatlabConnectionException
    {
        List<String> processArguments = new ArrayList<String>();
        
        //Location of MATLAB
        if(_options.getMatlabLocation() != null)
        {
            processArguments.add(_options.getMatlabLocation());
        }
        else
        {
            processArguments.add(Configuration.getMatlabLocation());
        }
        
        //MATLAB flags
        if(_options.getHidden())
        {
            if(Configuration.isWindows())
            {
                processArguments.add("-automation");
            }
            else
            {
                processArguments.add("-nosplash");
                processArguments.add("-nodesktop");
            }
        }
        else
        {
            if(Configuration.isOSX() || Configuration.isLinux())
            {
                processArguments.add("-desktop");
            }
        }
        
        //Code to run on startup
        processArguments.add("-r");
        processArguments.add(runArg);
        
        return processArguments; 
    }
    
    /**
     * Initializes the registry if it has not already been set up. Specifies the codebase so that paths with spaces in
     * them will work properly.
     * 
     * @throws MatlabConnectionException
     */
    private static void initRegistry() throws MatlabConnectionException
    {
        //If the registry hasn't been created
        if(_registry == null)
        {   
            //Create a RMI registry
            try
            {
                _registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            }
            //If we can't create one, try to retrieve an existing one
            catch(Exception e)
            {
                try
                {
                    _registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
                }
                catch(Exception ex)
                {
                    throw new MatlabConnectionException("Could not create or connect to the RMI registry", ex);
                }
            }
        }
    }
    
    /**
     * Receives the wrapper around JMI from MATLAB.
     */
    private class ProxyReceiver implements JMIWrapperRemoteReceiver
    {
        private final RequestCallback _requestCallback;
        
        private final String _receiverID;
        
        public ProxyReceiver(RequestCallback requestCallback)
        {
            _requestCallback = requestCallback;
            
            _receiverID = "PROXY_RECEIVER_" + getRandomValue();
        }
        
        @Override
        public void registerControl(String proxyID, JMIWrapperRemote jmiWrapper, boolean existingSession)
        {   
            //Remove self from the mapping of receivers
            _receivers.remove(proxyID); 
            
            //Create proxy, store it
            RemoteMatlabProxy proxy = new RemoteMatlabProxy(jmiWrapper, this, proxyID, existingSession);
            
            //Notify the callback
            _requestCallback.proxyCreated(proxy);
        }

        @Override
        public String getReceiverID()
        {
            return _receiverID;
        }
    }
    
    /**
     * Generates a random value to be used in binding and proxy IDs.
     * 
     * @return random value
     */
    private static String getRandomValue()
    {
        return UUID.randomUUID().toString();
    }
    
    private static String getRandomProxyID()
    {
        return "PROXY_REMOTE_" + getRandomValue();
    }
    
    @Override
    public String requestProxy(RequestCallback requestCallback) throws MatlabConnectionException
    {
        //Generate random ID for the proxy
        String proxyID = getRandomProxyID();
        
        //Initialize the registry (does nothing if already initialized)
        initRegistry();
        
        //Create and bind the receiver
        ProxyReceiver receiver = new ProxyReceiver(requestCallback);
        _receivers.put(proxyID, receiver);
        try
        {
            _registry.bind(receiver.getReceiverID(), UnicastRemoteObject.exportObject(receiver, 0));
        }
        catch(RemoteException ex)
        {
            _receivers.remove(proxyID);
            throw new MatlabConnectionException("Could not bind proxy receiver to the RMI registry", ex);
        }
        catch(AlreadyBoundException ex)
        {
            _receivers.remove(proxyID);
            throw new MatlabConnectionException("Could not bind proxy receiver to the RMI registry", ex);
        }
        
        //Connect to MATLAB
        try
        {
            //Attempt to locate a running session that can be connected to, if options permit doing so
            MatlabSession session = null;
            if(_options.getUseRunningSession())
            {
                session = getRunningSession();
            }

            //If there is a running session available for connection
            if(session != null)
            {
                try
                {
                    session.connectFromRMI(receiver.getReceiverID(), proxyID);
                }
                catch(RemoteException e)
                {
                    throw new MatlabConnectionException("Could not connect to running MATLAB session", e);
                }
            }
            //Launch a new session of MATLAB
            else
            {
                //Argument that MATLAB will run on start. Tells MATLAB to:
                // - Adds matlabcontrol to MATLAB's dynamic class path
                // - Adds matlabcontrol to Java's system class loader's class path (to work with RMI properly)
                // - Removes matlabcontrol from MATLAB's dynamic class path
                // - Tells matlabcontrol running in MATLAB to establish the connection to this JVM
                String runArg = "javaaddpath '" + Configuration.getSupportCodeLocation() + "'; " + 
                        MatlabClassLoaderHelper.class.getName() + ".configureClassLoading(); " +
                        "javarmpath '" + Configuration.getSupportCodeLocation() + "'; " +
                        MatlabConnector.class.getName() + ".connectFromMatlab('" + receiver.getReceiverID() + 
                        "', '" + proxyID + "');";

                //Build complete arguments to run MATLAB
                List<String> args = buildProcessArguments(runArg); 

                //Attempt to run MATLAB
                try
                {   
                    ProcessBuilder builder = new ProcessBuilder(args);
                    builder.start();
                }
                catch(IOException e)
                {
                    throw new MatlabConnectionException("Could not launch MATLAB. Process arguments: " + args, e);
                }
            }
        }
        catch(MatlabConnectionException e)
        {
            cleanupProxyRequest(proxyID);
            
            throw e;
        }
        
        return proxyID;
    }

    @Override
    public MatlabProxy getProxy() throws MatlabConnectionException
    {        
        //Request proxy
        GetProxyRequestCallback callback = new GetProxyRequestCallback();
        String proxyID = this.requestProxy(callback);
        
        try
        {   
            //TODO: What if proxy is received before putting thread to sleep?
            
            //Wait until the proxy is received or until timeout
            try
            {
                Thread.sleep(_options.getProxyTimeout());
            }
            catch(InterruptedException e)
            {
                //If interrupted, it should be because the proxy has been returned - if not throw an exception
                if(callback.getProxy() == null)
                {
                    throw new MatlabConnectionException("Thread was interrupted while waiting for MATLAB proxy", e);
                }
            }

            //If the proxy has not be received before the timeout
            if(callback.getProxy() == null)
            {
                throw new MatlabConnectionException("MATLAB proxy could not be created in " +
                        _options.getProxyTimeout() + " milliseconds");
            }

            return callback.getProxy();
        }
        catch(MatlabConnectionException e)
        {
            cleanupProxyRequest(proxyID);
            
            throw e;
        }
    }
    
    private void cleanupProxyRequest(String proxyID) throws MatlabConnectionException
    {
        ProxyReceiver receiver = _receivers.remove(proxyID);
        
        if(receiver != null)
        {
            try
            {
                UnicastRemoteObject.unexportObject(receiver, true);
            }
            catch(NoSuchObjectException e)
            {
                throw new MatlabConnectionException("Unable to remove proxy's receiver", e);
            }
        }
    }
    
    private static class GetProxyRequestCallback implements RequestCallback
    {
        private final Thread _requestingThread;
        private MatlabProxy _proxy;
        
        public GetProxyRequestCallback()
        {
            _requestingThread = Thread.currentThread();
        }

        @Override
        public void proxyCreated(MatlabProxy proxy)
        {
            _proxy = proxy;
            
            _requestingThread.interrupt();
        }
        
        public MatlabProxy getProxy()
        {
            return _proxy;
        }
    }
    
    /**
     * Returns a session that is available for connection. If no session is available, {@code null} will be returned.
     * 
     * @return 
     */
    private MatlabSession getRunningSession()
    {
        MatlabSession availableSession = null;
        
        try
        {
            Registry registry = LocateRegistry.getRegistry(MatlabBroadcaster.MATLAB_SESSION_PORT);
            
            String[] remoteNames = registry.list();
            for(String name : remoteNames)
            {
                if(name.startsWith(MatlabBroadcaster.MATLAB_SESSION_PREFIX))
                {
                    MatlabSession session = (MatlabSession) registry.lookup(name);
                    if(session.isAvailableForConnection())
                    {
                        availableSession = session;
                        break;
                    }
                }
            }
        }
        catch(Exception e) { }
        
        return availableSession;
    }
}