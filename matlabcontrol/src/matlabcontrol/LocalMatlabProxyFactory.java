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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates local instances of {@link MatlabProxy}.
 */
class LocalMatlabProxyFactory implements ProxyFactory
{
    private boolean _isShutdown = false;
    
    private final LocalMatlabProxy _proxy;
    private final MatlabConnectionListenerManager _listenerManager = new MatlabConnectionListenerManager();
    private final ExecutorService _requestExecutor = Executors.newSingleThreadExecutor();
    
    public LocalMatlabProxyFactory(MatlabProxyFactoryOptions.ImmutableFactoryOptions options)
    {
        JMIWrapper wrapper = MatlabConnector.getJMIWrapper();
        String proxyID = MatlabConnector.getProxyID();

        if(wrapper == null)
        {
            wrapper = new JMIWrapper();
            proxyID = "local";
        }
        else
        {
            proxyID = "local-" + proxyID;
        }

        _proxy = new LocalMatlabProxy(wrapper, proxyID, this);
    }
    
    @Override
    public synchronized LocalMatlabProxy getProxy() throws MatlabConnectionException
    {
        if(_isShutdown)
        {
            throw new MatlabConnectionException("This factory has been shutdown");
        }
        
        _listenerManager.connectionEstablished(_proxy);
                    
        return _proxy;
    }
    
    @Override
    public synchronized LocalMatlabProxy getProxy(long timeout) throws MatlabConnectionException
    {
        return this.getProxy();
    }
    
    @Override
    public synchronized String requestProxy() throws MatlabConnectionException
    {        
        if(_isShutdown)
        {
            throw new MatlabConnectionException("This factory has been shutdown");
        }
    
        //Notify the creation of the proxy after a 100ms delay so that the proxy's identifier can be returned first
        _requestExecutor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(100);
                    _listenerManager.connectionEstablished(_proxy);
                    
                }
                catch(InterruptedException ex) { }
            }
        });
        
        return _proxy.getIdentifier();
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
    public synchronized void shutdown()
    {
        if(!_isShutdown)
        {
            _isShutdown = true;
            
            _listenerManager.connectionLost(_proxy);

            _listenerManager.shutdown();
            _requestExecutor.shutdown();
        }
    }
    
    @Override
    public synchronized boolean isShutdown()
    {
        return _isShutdown;
    }
}