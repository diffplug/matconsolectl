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

import java.util.concurrent.atomic.AtomicInteger;

import matlabcontrol.MatlabProxy.Identifier;
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
    public LocalMatlabProxyFactory(MatlabProxyFactoryOptions options) { }
    
    @Override
    public LocalMatlabProxy getProxy() throws MatlabConnectionException
    {   
        JMIValidator.validateJMIMethods();
        
        return new LocalMatlabProxy(new LocalIdentifier());
    }
    
    @Override
    public Request requestProxy(RequestCallback requestCallback) throws MatlabConnectionException
    {   
        LocalMatlabProxy proxy = getProxy();
        requestCallback.proxyCreated(proxy);
        
        return new LocalRequest(proxy.getIdentifier());
    }
    
    private static final class LocalIdentifier implements Identifier
    {
        private static final AtomicInteger PROXY_CREATION_COUNTER = new AtomicInteger();
        
        private final int _id = PROXY_CREATION_COUNTER.getAndIncrement();
        
        @Override
        public boolean equals(Object other)
        {
            boolean equals;
            
            if(other instanceof LocalIdentifier)
            {
                equals = (((LocalIdentifier) other)._id == _id);
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
            return _id;
        }
        
        @Override
        public String toString()
        {
            return "PROXY_LOCAL_" + _id;
        }
    }
    
    private static final class LocalRequest implements Request
    {
        private final Identifier _proxyID;
        
        private LocalRequest(Identifier proxyID)
        {
            _proxyID = proxyID;
        }

        @Override
        public Identifier getProxyIdentifer()
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