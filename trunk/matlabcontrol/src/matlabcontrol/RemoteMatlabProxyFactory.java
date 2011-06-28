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
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import matlabcontrol.MatlabProxy.Identifier;
import matlabcontrol.MatlabProxyFactory.Request;
import matlabcontrol.MatlabProxyFactory.RequestCallback;

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
    private final MatlabProxyFactoryOptions _options;
    
    /**
     * {@link Receiver} instances. They need to be stored because the RMI registry only holds weak references to
     * exported objects.
     */
    private final CopyOnWriteArrayList<Receiver> _receivers = new CopyOnWriteArrayList<Receiver>();
    
    /**
     * The RMI registry used to communicate between JVMs.
     */
    private Registry _registry = null;
    
    public RemoteMatlabProxyFactory(MatlabProxyFactoryOptions options)
    {
        _options = options;
    }
    
    @Override
    public Request requestProxy(RequestCallback requestCallback) throws MatlabConnectionException
    {
        //Unique identifier for the proxy
        RemoteIdentifier proxyID = new RemoteIdentifier();
        
        Request request;
        
        //Initialize the registry (does nothing if already initialized)
        initRegistry();
        
        //Create and bind the receiver
        Receiver receiver = new Receiver(requestCallback, proxyID, Configuration.getClassPathAsRMICodebase());
        _receivers.add(receiver);
        try
        {
            _registry.bind(receiver.getReceiverID(), LocalHostRMIHelper.exportObject(receiver));
        }
        catch(RemoteException ex)
        {
            _receivers.remove(receiver);
            throw new MatlabConnectionException("Could not bind proxy receiver to the RMI registry", ex);
        }
        catch(AlreadyBoundException ex)
        {
            _receivers.remove(receiver);
            throw new MatlabConnectionException("Could not bind proxy receiver to the RMI registry", ex);
        }
        
        //Connect to MATLAB
        try
        {
            //If allowed to connect to a running session and a connection could be made
            if(_options.getUseRunningSession() && MatlabSessionImpl.connectToRunningSession(receiver.getReceiverID(),
                    _options.getRMIPort()))
            {
                request = new RemoteRequest(proxyID, null, receiver);
            }
            //Else, launch a new session of MATLAB
            else
            {
                Process process = createProcess(receiver);
                request = new RemoteRequest(proxyID, process, receiver);
            }
        }
        catch(MatlabConnectionException e)
        {
            receiver.shutdown();
            throw e;
        }
        
        return request;
    }

    @Override
    public MatlabProxy getProxy() throws MatlabConnectionException
    {
        //Request proxy
        GetProxyRequestCallback callback = new GetProxyRequestCallback();
        Request request = this.requestProxy(callback);
        
        try
        {   
            //It is possible (although very unlikely) for the proxy to have been created before the following call to
            //sleep occurs. If this happens then the proxy will not be returned until the timeout is reached.
            
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
            request.cancel();
            throw e;
        }
    }
    
    /**
     * Initializes the registry if it has not already been set up.
     * 
     * @throws MatlabConnectionException
     */
    private synchronized void initRegistry() throws MatlabConnectionException
    {
        //If the registry hasn't been created
        if(_registry == null)
        {   
            //Create a RMI registry
            try
            {
                _registry = LocalHostRMIHelper.createRegistry(_options.getRMIPort());
            }
            //If we can't create one, try to retrieve an existing one
            catch(Exception e)
            {
                try
                {
                    _registry = LocalHostRMIHelper.getRegistry(_options.getRMIPort());
                }
                catch(Exception ex)
                {
                    throw new MatlabConnectionException("Could not create or connect to the RMI registry", ex);
                }
            }
        }
    }
    
    /**
     * Uses the {@link #_options} and the arguments to create a {@link Process} that will launch MATLAB and
     * connect it to this JVM.
     * 
     * @param receiver
     * @return
     * @throws MatlabConnectionException 
     */
    private Process createProcess(Receiver receiver) throws MatlabConnectionException
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
        
        if(_options.getLicenseFile() != null)
        {
            processArguments.add("-c");
            processArguments.add(_options.getLicenseFile());
        }
        
        if(_options.getLogFile() != null)
        {
            processArguments.add("-logfile");
            processArguments.add(_options.getLogFile());
        }
        
        if(_options.getJavaDebugger() != null)
        {
            processArguments.add("-jdb");
            processArguments.add(_options.getJavaDebugger().toString());
        }
        
        if(_options.getUseSingleComputationalThread())
        {
            processArguments.add("-singleCompThread");
        }
        
        //Code to run on startup
        processArguments.add("-r");
        
        //Argument that MATLAB will run on start. Tells MATLAB to:
        // - Adds matlabcontrol to MATLAB's dynamic class path
        // - Adds matlabcontrol to Java's system class loader's class path (to work with RMI properly)
        // - Removes matlabcontrol from MATLAB's dynamic class path
        // - Tells matlabcontrol running in MATLAB to establish the connection to this JVM
        String runArg = "javaaddpath '" + Configuration.getSupportCodeLocation() + "'; " + 
                        MatlabClassLoaderHelper.class.getName() + ".configureClassLoading(); " +
                        "javarmpath '" + Configuration.getSupportCodeLocation() + "'; " +
                        MatlabConnector.class.getName() + ".connectFromMatlab('" + receiver.getReceiverID() + "', " +
                            _options.getRMIPort() + ");";
        processArguments.add(runArg);
        
        //Create process
        ProcessBuilder builder = new ProcessBuilder(processArguments);
        try
        {
            return builder.start();
        }
        catch(IOException e)
        {
            throw new MatlabConnectionException("Could not launch MATLAB. Command: " + builder.command(), e);
        }
    }
    
    /**
     * Receives a wrapper around JMI from MATLAB.
     */
    private class Receiver implements JMIWrapperRemoteReceiver
    {
        private final RequestCallback _requestCallback;
        private final RemoteIdentifier _proxyID;
        private final String _codebase;
        
        private final String _receiverID;
        
        private volatile boolean _receivedJMIWrapper = false;
        
        public Receiver(RequestCallback requestCallback, RemoteIdentifier proxyID, String codebase)
        {
            _requestCallback = requestCallback;
            _proxyID = proxyID;
            _codebase = codebase;
            
            _receiverID = "PROXY_RECEIVER_" + proxyID.getUUIDString();
        }
        
        @Override
        public void receiveJMIWrapper(JMIWrapperRemote jmiWrapper, boolean existingSession)
        {   
            //Remove self from the list of receivers
            _receivers.remove(this); 
            
            //Create proxy
            RemoteMatlabProxy proxy = new RemoteMatlabProxy(jmiWrapper, this, _proxyID, existingSession);
            proxy.init();
            
            //Record wrapper has been received
            _receivedJMIWrapper = true;
            
            //Notify the callback
            _requestCallback.proxyCreated(proxy);
        }

        @Override
        public String getReceiverID()
        {
            return _receiverID;
        }
        
        public boolean shutdown()
        {
            _receivers.remove(this);
             
            boolean success;
            try
            {
                success = UnicastRemoteObject.unexportObject(this, true);
            }
            catch(NoSuchObjectException e)
            {
                success = true;
            }
            
            return success;
        }
        
        public boolean hasReceivedJMIWrapper()
        {
            return _receivedJMIWrapper;
        }

        @Override
        public String getRMICodebase() throws RemoteException
        {
            return _codebase;
        }
    }
    
    private static class GetProxyRequestCallback implements RequestCallback
    {
        private final Thread _requestingThread;
        private volatile MatlabProxy _proxy;
        
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
    
    private static final class RemoteIdentifier implements Identifier
    {
        private final UUID _id;
        
        private RemoteIdentifier()
        {
            _id = UUID.randomUUID();
        }
        
        private RemoteIdentifier(String uuidString)
        {
            _id  = UUID.fromString(uuidString);
        }
        
        @Override
        public boolean equals(Object other)
        {
            boolean equals;
            
            if(other instanceof RemoteIdentifier)
            {
                equals = ((RemoteIdentifier) other)._id.equals(_id);
            }
            else
            {
                equals = false;
            }
            
            return equals;
        }

        @Override
        public int hashCode()
        {
            return _id.hashCode();
        }
        
        @Override
        public String toString()
        {
            return "PROXY_REMOTE_" + _id;
        }
        
        public String getUUIDString()
        {
            return _id.toString();
        }
    }
    
    private static class RemoteRequest implements Request
    {
        private final Identifier _proxyID;
        private final Process _process;
        private final Receiver _receiver;
        private boolean _isCancelled = false;
        
        private RemoteRequest(Identifier proxyID, Process process, Receiver receiver)
        {
            _proxyID = proxyID;
            _process = process;
            _receiver = receiver;
        }
        
        @Override
        public Identifier getProxyIdentifer()
        {
            return _proxyID;
        }

        @Override
        public synchronized boolean cancel()
        {
            if(!_isCancelled)
            {
                boolean success;
                if(!this.isCompleted())
                {
                    if(_process != null)
                    {
                        _process.destroy();
                    }
                    success = _receiver.shutdown();
                }
                else
                {
                    success = false;
                }
                _isCancelled = success;
            }
            
            return _isCancelled;
        }

        @Override
        public synchronized boolean isCancelled()
        {
            return _isCancelled;
        }

        @Override
        public boolean isCompleted()
        {
            return _receiver.hasReceivedJMIWrapper();
        }
    }
}