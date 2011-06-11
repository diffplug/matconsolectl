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

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Enables MATLAB to be seen by matlabcontrol. This class may only be used from inside MATLAB.
 * 
 * @see #broadcast()
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabBroadcaster
{
    /**
     * The port used by RMI to store information about MATLAB sessions.
     */
    static final int MATLAB_SESSION_PORT = Registry.REGISTRY_PORT + 1000;
    
    /**
     * The prefix for all RMI names of bound instances of {@link MatlabSession}.
     */
    static final String MATLAB_SESSION_PREFIX = "MATLAB_SESSION_";
    
    /**
     * A reference to the RMI registry which holds {@code MatlabSession}s.
     */
    private static Registry _registry = null;
    
    /**
     * Receivers that have been retrieved from Java programs running outside of MATLB>
     */
    private static final List<JMIWrapperRemoteReceiver> _receivers = new ArrayList<JMIWrapperRemoteReceiver>();
    
    /**
     * The bound name in the RMI registry for the instance of {@code MatlabSession} that represents this session of
     * MATLAB.
     */
    private static final String _sessionID = MATLAB_SESSION_PREFIX + UUID.randomUUID().toString();
    
    /**
     * Represents this session of MATLAB.
     */
    private static final MatlabSession _session = new MatlabSessionImpl();
    
    /**
     * The frequency (in milliseconds) with which to check if the connection to the registry still exists.
     */
    private static final int BROADCAST_CHECK_PERIOD = 1000;
    
    /**
     * The timer used to check if still connected to the registry.
     */
    private static Timer _broadcastTimer;
    
    /**
     * Private constructor so this class cannot be constructed.
     */
    private MatlabBroadcaster() { }
    
    /**
     * Makes this session of MATLAB visible to matlabcontrol. Once broadcasting, matlabcontrol running outside MATLAB
     * will be able to connect to this session of MATLAB.
     * <br><br>
     * When a session of MATLAB is launched by matlabcontrol it is automatically configured to broadcast.
     * 
     * @throws MatlabConnectionException 
     */
    public synchronized static void broadcast() throws MatlabConnectionException
    {   
        //Only allow this class to be used from with MATLAB
        if(!Configuration.isRunningInsideMatlab())
        {
            throw new MatlabConnectionException(MatlabBroadcaster.class.getName() + " may only be created inside MATLAB");
        }
        
        //If the registry hasn't been created
        if(_registry == null)
        {   
            //Configure class loading as necessary to work properly with RMI
            MatlabClassLoaderHelper.configureClassLoading();
            
            //Create or retrieve an RMI registry
            setupRegistry();

            //Tell the code base where it is and force it to use it exclusively
            //This is necessary so that paths with spaces work properly
            System.setProperty("java.rmi.server.codebase", Configuration.getCodebaseLocation());
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");
            
            //Register this session so that it can be reconnected to
            bindSession();
            
            //If the registry becomes disconnected, either create a new one or locate a new one
            maintainRegistryConnection();
        }
    }
    
    /**
     * Attempts to create a registry, and if that cannot be done, then attempts to get an existing registry.
     * 
     * @throws MatlabConnectionException if a registry can neither be created nor retrieved
     */
    private static void setupRegistry() throws MatlabConnectionException
    {
        try
        {
            _registry = LocateRegistry.createRegistry(MATLAB_SESSION_PORT);
        }
        //If we can't create one, try to retrieve an existing one
        catch(Exception e)
        {
            try
            {
                _registry = LocateRegistry.getRegistry(MATLAB_SESSION_PORT);
            }
            catch(Exception ex)
            {
                throw new MatlabConnectionException("Could not create or connect to the RMI registry", ex);
            }
        }
    }
    
    /**
     * Binds the session object, an instance of {@link MatlabSession} to the registry with {@link #_sessionID}.
     * 
     * @throws MatlabConnectionException 
     */
    private static void bindSession() throws MatlabConnectionException
    {
        //Unexport the object, it will throw an exception if it is not bound - so ignore that
        try
        {
            UnicastRemoteObject.unexportObject(_session, true);
        }
        catch(NoSuchObjectException e) { }
        
        try
        {   
            _registry.bind(_sessionID, UnicastRemoteObject.exportObject(_session, 0));
        }
        catch(Exception e)
        {
            throw new MatlabConnectionException("Could not register this session of MATLAB", e);
        }
    }
    
    /**
     * Keeps track of the {@code receiver}.
     * 
     * @param receiver 
     */
    synchronized static void addReceiver(JMIWrapperRemoteReceiver receiver)
    {
        _receivers.add(receiver);
    }
    
    /**
     * Determines if there are any {@link JMIWrapperRemoteReceiver} currently connected to this session of MATLAB. A
     * receiver gets added each time a remote proxy is connected. A receiver can become disconnected if RMI is shut
     * down where the the remote proxy exists (or if the application is terminated). If the receiver is connected then
     * the proxy should also be connected. Therefore if a receiver is connected, a remote proxy is connected.
     * 
     * @return if a receiver is connected
     */
    synchronized static boolean areAnyReceiversConnected()
    {
        //Remove all receivers that are no longer connected
        List<JMIWrapperRemoteReceiver> disconnectedReceivers = new ArrayList<JMIWrapperRemoteReceiver>();
        for(JMIWrapperRemoteReceiver receiver : _receivers)
        {
            try
            {
                receiver.getReceiverID();
            }
            catch(RemoteException e)
            {
                disconnectedReceivers.add(receiver);
            }
        }
        _receivers.removeAll(disconnectedReceivers);
        
        return !_receivers.isEmpty();
    }
    
    /**
     * Checks with a timer that the registry still exists and that the session object is exported to it. If either
     * stop being the case then an attempt is made to re-establish.
     */
    private static void maintainRegistryConnection()
    {
        //Create a timer to monitor the broadcast
        _broadcastTimer = new Timer();
        _broadcastTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                //Check if the registry is connected
                try
                {
                    //Will succeed if connected and the session object is still exported
                    _registry.lookup(_sessionID);
                }
                //Session object is no longer exported
                catch(NotBoundException e)
                {
                    try
                    {
                        bindSession();
                    }
                    //Nothing more can be done if this fails
                    catch(MatlabConnectionException ex) { }
                }
                //Registry is no longer connected
                catch(RemoteException e)
                {
                    try
                    {
                        setupRegistry();
                        bindSession();
                    }
                    //Nothing more can be done if this fails
                    catch(MatlabConnectionException ex) { }
                }
            }
        }, BROADCAST_CHECK_PERIOD, BROADCAST_CHECK_PERIOD);
    }
}