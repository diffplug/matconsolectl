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
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Enables a session of MATLAB to be connected to by matlabcontrol running outside MATLAB.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabBroadcaster
{
    /**
     * A reference to the RMI registry which holds {@code MatlabSession}s.
     */
    private static Registry _registry = null;
    
    /**
     * Represents this session of MATLAB.
     */
    private static final MatlabSessionImpl _session = new MatlabSessionImpl();
    
    /**
     * The frequency (in milliseconds) with which to check if the connection to the registry still exists.
     */
    private static final int BROADCAST_CHECK_PERIOD = 1000;
    
    /**
     * The timer used to check if still connected to the registry.
     */
    private static final Timer _broadcastTimer = new Timer();
    
    /**
     * Private constructor so this class cannot be constructed.
     */
    private MatlabBroadcaster() { }

    /**
     * Returns the session object bound to the RMI registry by this broadcaster.
     * 
     * @return 
     */
    static MatlabSessionImpl getSession()
    {
        return _session;
    }
    
    /**
     * Makes this session of MATLAB visible to matlabcontrol. Once broadcasting, matlabcontrol running outside MATLAB
     * will be able to connect to this session of MATLAB.
     * 
     * @throws MatlabConnectionException thrown if not running inside MATLAB or unable to broadcast
     */
    synchronized static void broadcast(int broadcastPort) throws MatlabConnectionException
    {   
        //If the registry hasn't been created
        if(_registry == null)
        {   
            //Create or retrieve an RMI registry
            setupRegistry(broadcastPort);
            
            //Register this session so that it can be reconnected to
            bindSession();
            
            //If the registry becomes disconnected, either create a new one or locate a new one
            maintainRegistryConnection(broadcastPort);
        }
    }
    
    /**
     * Attempts to create a registry, and if that cannot be done, then attempts to get an existing registry.
     * 
     * @throws MatlabConnectionException if a registry can neither be created nor retrieved
     */
    private static void setupRegistry(int broadcastPort) throws MatlabConnectionException
    {
        try
        {
            _registry = LocalHostRMIHelper.createRegistry(broadcastPort);
        }
        //If we can't create one, try to retrieve an existing one
        catch(Exception e)
        {
            try
            {
                _registry = LocalHostRMIHelper.getRegistry(broadcastPort);
            }
            catch(Exception ex)
            {
                throw new MatlabConnectionException("Could not create or connect to the RMI registry", ex);
            }
        }
    }
    
    /**
     * Binds the session object, an instance of {@link MatlabSession} to the registry with {@link #SESSION_ID}.
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
            _registry.bind(_session.getSessionID(), LocalHostRMIHelper.exportObject(_session));
        }
        catch(Exception e)
        {
            throw new MatlabConnectionException("Could not register this session of MATLAB", e);
        }
    }
    
    /**
     * Checks with a timer that the registry still exists and that the session object is exported to it. If either
     * stop being the case then an attempt is made to re-establish.
     */
    private static void maintainRegistryConnection(final int broadcastPort)
    {
        //Configure the a timer to monitor the broadcast
        _broadcastTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                //Check if the registry is connected
                try
                {
                    //Will succeed if connected and the session object is still exported
                    _registry.lookup(_session.getSessionID());
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
                        setupRegistry(broadcastPort);
                        bindSession();
                    }
                    //Nothing more can be done if this fails
                    catch(MatlabConnectionException ex) { }
                }
            }
        }, BROADCAST_CHECK_PERIOD, BROADCAST_CHECK_PERIOD);
    }
}