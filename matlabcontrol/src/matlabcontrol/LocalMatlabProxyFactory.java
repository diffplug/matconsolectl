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

import java.util.concurrent.atomic.AtomicLong;

import matlabcontrol.MatlabProxyFactory.Request;
import matlabcontrol.MatlabProxyFactory.RequestCallback;

/**
 * Creates local instances of {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalMatlabProxyFactory implements ProxyFactory
{
    private static final AtomicLong PROXY_CREATION_COUNT = new AtomicLong();
    
    public LocalMatlabProxyFactory(MatlabProxyFactoryOptions.ImmutableFactoryOptions options)
    {

    }
    
    @Override
    public LocalMatlabProxy getProxy() throws MatlabConnectionException
    {
        String proxyID = "PROXY_LOCAL_" + PROXY_CREATION_COUNT.getAndIncrement();
        LocalMatlabProxy proxy = new LocalMatlabProxy(MatlabConnector.getJMIWrapper(), proxyID);
        
        return proxy;
    }
    
    @Override
    public Request requestProxy(RequestCallback requestCallback) throws MatlabConnectionException
    {   
        LocalMatlabProxy proxy = getProxy();
        requestCallback.proxyCreated(proxy);
        
        return new LocalRequest(proxy.getIdentifier());
    }
    
    private static final class LocalRequest implements Request
    {
        private final String _proxyID;
        
        private LocalRequest(String proxyID)
        {
            _proxyID = proxyID;
        }

        @Override
        public String getProxyIdentifer()
        {
            return _proxyID;
        }

        @Override
        public boolean cancel()
        {
            return false;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isCompleted()
        {
            return true;
        }
    }
}