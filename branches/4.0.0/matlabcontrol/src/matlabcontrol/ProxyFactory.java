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

import matlabcontrol.MatlabProxyFactory.Request;
import matlabcontrol.MatlabProxyFactory.RequestCallback;

/**
 * A factory which creates instances of {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface ProxyFactory
{
    /**
     * Returns a {@link MatlabProxy}. If a connection cannot be established before the timeout then this method will end
     * execution and an exception will be thrown. A timeout can be specified with the options provided to this factory.
     * If no timeout was specified, then a default of 180 seconds will be used.
     * <br><br>
     * While this method blocks the calling thread until a proxy is created (or the timeout is reached), any number of
     * threads may call {@code getProxy()} simultaneously.
     * 
     * @throws MatlabConnectionException
     * @return proxy
     */
    public MatlabProxy getProxy() throws MatlabConnectionException;
    
    /**
     * Requests a {@link MatlabProxy}. When the proxy has been created it will be provided to the {@code callback}. The
     * proxy may be provided to the callback before this method returns. There is no timeout. The returned
     * {@link Request} instance provides information about the request and can be used to cancel the request.
     * <br><br>
     * This method is non-blocking. Any number of requests may be made simultaneously from the same thread or different
     * threads.
     * 
     * @throws MatlabConnectionException
     * @return request
     */
    public Request requestProxy(RequestCallback callback) throws MatlabConnectionException;
}