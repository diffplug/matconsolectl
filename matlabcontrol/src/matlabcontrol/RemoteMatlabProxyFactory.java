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
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Creates remote instances of {@link MatlabProxy}. Creating a proxy will launch MATLAB. Each proxy created will control
 * the session launched. This factory can be used to create any number of proxies.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class RemoteMatlabProxyFactory implements ProxyFactory
{
    /**
     * The options that configure this instance of the factory.
     */
    private final MatlabProxyFactoryOptions.ImmutableFactoryOptions _options;
    
    /**
     * A timer that periodically checks if the proxies are still connected.
     */
    private Timer _connectionTimer;
    
    /**
     * Manages {@link MatlabConnectionListener}s.
     */
    private final MatlabConnectionListenerManager _listenerManager = new MatlabConnectionListenerManager();
    
    /**
     * The arguments that will be used to launch MATLAB.
     */
    private final List<String> _processArguments;
    
    /**
     * The location of this support code. This location is provided to MATLAB so that it can add the location of this
     * code to its classpath.
     */
    private final String _supportCodeLocation;
    
    /**
     * Default number of milliseconds to wait for a {@link JMIWrapperRemote} to be received.
     */
    private static final int DEFAULT_TIMEOUT = 60000;
    
    /**
     * The duration (in milliseconds) between checks to determine if the proxies are still connected.
     */
    private static final int CONNECTION_CHECK_PERIOD = 1000;
    
    /**
     * Map of proxyIDs to the threads that are sleeping and awaiting to return a proxy.
     */
    private final ConcurrentMap<String, Thread> _identifierToThread = new ConcurrentHashMap<String, Thread>();
    
    /**
     * Map of proxyIDs to {@link RemoteMatlabProxy} instances.
     */
    private final ConcurrentMap<String, RemoteMatlabProxy> _proxies = new ConcurrentHashMap<String, RemoteMatlabProxy>();
    
    /**
     * The RMI registry used to communicate between JVMs. There is only ever one registry actually running on a given
     * machine, so multiple distinct programs making use of matlabcontrol all share the same underlying registry
     * (although the Java object will be different).
     */
    private static Registry _registry = null;
    
    /**
     * Receiver for proxies created and sent over RMI.
     */
    private final JMIWrapperRemoteReceiver _receiver = new ProxyReceiver();
    
    /**
     * Value used to bind the {@link ProxyReceiver}, as a {@link JMIWrapperRemoteReceiver} so that it can be
     * retrieved from within the MATLAB JVM with this value.
     */
    private final String _receiverID = "REMOTE_RECEIVER_" + getRandomValue();
    
    /**
     * Whether this factory has been shutdown via {@link #shutdown()}. Once the factory has been shutdown, all
     * subsequent calls to create proxies will fail.
     */
    private boolean _isShutdown = false;
    
    public RemoteMatlabProxyFactory(MatlabProxyFactoryOptions.ImmutableFactoryOptions options) throws MatlabConnectionException
    {
        _options = options;
        
        //Build arguments that will launch MATLAB
        _processArguments = this.getProcessArguments(); 
        
        //Location of where this code is
        _supportCodeLocation = Configuration.getSupportCodeLocation();
        
        //Initialize the registry
        initRegistry();
        
        //Bind the receiver to be retrieved from MATLAB
        this.bindReceiver();
    }
    
    /**
     * Turns the {@code options} into arguments that launch MATLAB.
     * 
     * @param options
     * @return
     * @throws MatlabConnectionException 
     */
    private List<String> getProcessArguments() throws MatlabConnectionException
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
        
        return Collections.unmodifiableList(processArguments); 
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
     * Binds the receiver for RMI so it can be retrieved from the MATLAB JVM as a {@link JMIWrapperRemoteReceiver}.
     * 
     * @throws MatlabConnectionException
     */
    private void bindReceiver() throws MatlabConnectionException
    {
        try
        {
            _registry.bind(_receiverID, UnicastRemoteObject.exportObject(_receiver, 0));
        }
        catch(Exception e)
        {
            throw new MatlabConnectionException("Could not bind proxy receiver to the RMI registry", e);
        }
    }
    
    /**
     * Receives the inner proxy from MATLAB. This inner class exists to hide the
     * {@link JMIWrapperRemoteReceiver#registerControl(String, JMIWrapperRemote)} method which must be public
     * because it is implementing an interface; however, this method should not be visible to users of the API so
     * instead it is hidden inside of this private class.
     */
    private class ProxyReceiver implements JMIWrapperRemoteReceiver
    {
        @Override
        public void registerControl(String proxyID, JMIWrapperRemote jmiWrapper, boolean existingSession)
        {   
            //Wait for 2 seconds so that MATLAB can properly initialize.
            //Attempts to determine exactly when MATLAB is properly initialized have failed. This solution, while less
            //than ideal, appears to work in practice.
            try
            {
                Thread.sleep(2000);
            }
            catch(InterruptedException e) { }
            
            //Create proxy, store it
            RemoteMatlabProxy proxy = new RemoteMatlabProxy(jmiWrapper, proxyID, existingSession);
            _proxies.put(proxyID, proxy);
            
            //If there is a thread waiting for the proxy, wake it up
            if(_identifierToThread.containsKey(proxyID))
            {
                Thread thread = _identifierToThread.get(proxyID);
                _identifierToThread.remove(proxyID);
                thread.interrupt();
            }
            _listenerManager.connectionEstablished(proxy);
            
            //Create the timer, if necessary, which checks if proxies are still connected
            RemoteMatlabProxyFactory.this.initConnectionTimer();
        }

        @Override
        public String getReceiverID() throws RemoteException
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
        return "PROXY_" + getRandomValue();
    }
    
    @Override
    public String requestProxy() throws MatlabConnectionException
    {
        String proxyID = getRandomProxyID();
        this.requestProxy(proxyID);
        
        return proxyID;
    }
    
    private void requestProxy(String proxyID) throws MatlabConnectionException
    {
        if(_isShutdown)
        {
            throw new MatlabConnectionException("This factory has been shutdown");
        }
        
        //Attempt to locate a running session that can be connected to, if options permit doing so
        MatlabSession session = null;
        if(_options.getUseRunningSession())
        {
            session = getExistingSession();
        }
        
        //If there is a running session available for connection
        if(session != null)
        {
            try
            {
                session.connectFromRMI(_receiverID, proxyID);
            }
            catch(RemoteException e)
            {
                throw new MatlabConnectionException("Could not connect to running MATLAB session", e);
            }
        }
        //Launch a new session of MATLAB
        else
        {
            //Argument that MATLAB will run on start.
            //Tells MATLAB to add this code to its classpath, then to call a method which
            //will create a proxy and send it over RMI back to this JVM.
            String runArg = "javaaddpath '" + _supportCodeLocation + "'; " +
                            MatlabClassLoaderHelper.class.getName() + ".configureClassLoading;" + 
                            MatlabConnector.class.getName() + 
                            ".connectFromMatlab('" + _receiverID + "', '" + proxyID + "', false);";
            List<String> args = new ArrayList<String>(_processArguments);
            args.add(runArg);

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
    
    @Override
    public RemoteMatlabProxy getProxy() throws MatlabConnectionException
    {
        return this.getProxy(DEFAULT_TIMEOUT);
    }

    @Override
    public RemoteMatlabProxy getProxy(long timeout) throws MatlabConnectionException
    {        
        String proxyID = getRandomProxyID();
        
        //Associate the calling thread with the proxy to be created so that the thread can be woken up when the
        //proxy is received
        _identifierToThread.put(proxyID, Thread.currentThread());
        
        //Request proxy
        this.requestProxy(proxyID);
        
        //Wait until the controller is received or until timeout
        try
        {
            Thread.sleep(timeout);
        }
        catch(InterruptedException e)
        {
            //If interrupted, it should be because the proxy has been returned - if not throw an exception
            if(!_proxies.containsKey(proxyID))
            {
                throw new MatlabConnectionException("Thread was interrupted while waiting for MATLAB proxy", e);
            }
        }
                    
        //If the proxy has not be received before the timeout
        if(!_proxies.containsKey(proxyID))
        {
            throw new MatlabConnectionException("MATLAB proxy could not be created in the specified amount of time: " +
                    timeout + " milliseconds");
        }
        
        return _proxies.get(proxyID);
    }
    
    /**
     * Returns a session that is available for connection. If no session is available, {@code null} will be returned.
     * 
     * @return 
     */
    private MatlabSession getExistingSession()
    {
        MatlabSession availableSession = null;
        
        try
        {
            Registry registry = LocateRegistry.getRegistry(MatlabBroadcaster.MATLAB_SESSION_PORT);;
            
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
    
    @Override
    public void addConnectionListener(MatlabConnectionListener listener)
    {
        _listenerManager.addConnectionListener(listener);
    }
    
    @Override
    public void removeConnectionListener(MatlabConnectionListener listener)
    {
        _listenerManager.removeConnectionListener(listener);
    }
    
    @Override
    public void shutdown() throws MatlabConnectionException
    {      
        if(!_isShutdown)
        {
            //Unexport the receiver so that the RMI threads can shut down
            try
            {
                UnicastRemoteObject.unexportObject(_receiver, true);
            }
            catch(NoSuchObjectException e)
            {
                throw new MatlabConnectionException("Unable to deregister listener of incoming proxies", e);
            }

            //Wait for the proxies to disconnect so that listeners can be notified
            try
            {
                Thread.sleep(CONNECTION_CHECK_PERIOD);
            }
            catch(InterruptedException e)
            {
                throw new MatlabConnectionException("Unable to wait for proxies to disconnect", e);
            }

            //Stop other threads: shutdown the listener manager and stop the timer
            _listenerManager.shutdown();
            
            if(_connectionTimer != null)
            {
                _connectionTimer.cancel();
            }

            //Record shutdown
            _isShutdown = true;
        }
    }
    
    @Override
    public boolean isShutdown()
    {
        return _isShutdown;
    }
    
    /**
     * Creates a timer, if it does not already exist, to check for lost proxy connections.
     */    
    private void initConnectionTimer()
    {
        //If there is no timer yet and if the factory has not been shutdown
        if(_connectionTimer == null && !_isShutdown)
        {
            //Create a timer to monitor the connections
            _connectionTimer = new Timer();
            _connectionTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    //Check each proxy's connection, if it is no longer connected remove the reference
                    //and notify the listeners
                    Iterator<Entry<String, RemoteMatlabProxy>> iterator = _proxies.entrySet().iterator();
                    while(iterator.hasNext())
                    {
                        RemoteMatlabProxy proxy = iterator.next().getValue();
                        if(!proxy.isConnected())
                        {
                            iterator.remove();

                            //The notification occurs on a seperate thread so this will not slow down the loop
                            _listenerManager.connectionLost(proxy);
                        }
                    }
                }
            }, CONNECTION_CHECK_PERIOD, CONNECTION_CHECK_PERIOD);
        }
    }
}