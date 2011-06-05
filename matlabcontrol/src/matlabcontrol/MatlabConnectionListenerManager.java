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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages notification of {@link MatlabConnectionListener}s. This class is thread-safe.
 */
class MatlabConnectionListenerManager
{
    /**
     * Listeners for when connections are established and lost.
     * 
     * @see #addConnectionListener(MatlabConnectionListener)
     * @see #removeConnectionListener(MatlabConnectionListener)
     */
    private final List<MatlabConnectionListener> _listeners = new CopyOnWriteArrayList<MatlabConnectionListener>();
    
    /**
     * Used to notify listeners on a separate thread that a connection has been lost or gained. This is done so that
     * whatever code is executed by the  user of this API when they are notified of a connection being lost or received
     * does not interfere with the essential operations of creating and receiving proxies by holding up the thread.
     */
    private final ExecutorService _connectionExecutor = Executors.newSingleThreadExecutor();
    
    /**
     * Adds a listener to be notified when MATLAB connections are established and lost.
     * <br><br>
     * This will have no effect if this manager has been shutdown via {@link #shutdown()}.
     * 
     * @param listener
     */
    void addConnectionListener(MatlabConnectionListener listener)
    {
        _listeners.add(listener);
    }
    
    /**
     * Removes a listener so that it is no longer notified.
     * 
     * @param listener
     */
    void removeConnectionListener(MatlabConnectionListener listener)
    {
        _listeners.remove(listener);
    }
    
    /**
     * Called when a connection has been established.
     * <br><br>
     * Notify the listeners that the connection has been established in a separate thread so that it whatever users of
     * this API are doing it does not interfere.
     * 
     * @param proxy
     */
    void connectionEstablished(final MatlabProxy<Object> proxy)
    {
        _connectionExecutor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                for(MatlabConnectionListener listener : _listeners)
                {
                    listener.connectionEstablished(proxy);
                }
            }
        });
    }
    
    /**
     * Called when a connection has been lost.
     * <br><br>
     * Notify the listeners that the connection has been lost in a separate thread so that it whatever users of this API
     * are doing it does not interfere with checking the proxies.
     */
    void connectionLost(final MatlabProxy<Object> proxy)
    {
        _connectionExecutor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                for(MatlabConnectionListener listener : _listeners)
                {
                    listener.connectionLost(proxy);
                }
            }
        });
    }
    
    /**
     * Shuts down the manager so that it no longer notifies listeners.
     */
    void shutdown()
    {
        _connectionExecutor.shutdown();
    }
}