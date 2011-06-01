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

/**
 *
 */
interface ProxyCreator
{
    /**
     * Returns a {@link MatlabProxy}. Connection listeners will be notified upon successful creation. An exception will
     * be thrown if the factory has been shutdown.
     * <br><br>
     * <strong>Running inside MATLAB</strong><br>
     * The proxy will be returned nearly instantly.
     * <br><br>
     * <strong>Running outside MATLAB</strong><br>
     * This method will launch a new session of MATLAB. If a connection cannot be established within 60 seconds then
     * this method will end execution and an exception will be thrown.
     * 
     * @see #requestProxy()
     * @see #getProxy(long)
     * 
     * @throws MatlabConnectionException
     * 
     * @return proxy
     */
    public MatlabProxy getProxy() throws MatlabConnectionException;
    
    /**
     * Returns a {@link MatlabProxy}. Connection listeners will be notified upon successful creation. An exception will
     * be thrown if the factory has been shutdown.
     * <br><br>
     * <strong>Running inside MATLAB</strong><br>
     * The proxy will be returned nearly instantly. The {@code timeout} parameter is ignored; this method call is
     * identical to {@link #getProxy()}.
     * <br><br>
     * <strong>Running outside MATLAB</strong><br>
     * This method will launch a new session of MATLAB. If a connection cannot be established within the specified
     * number of milliseconds specified by {@code timeout} then this method will end execution and an exception will be
     * thrown.
     * 
     * @see #requestProxy()
     * @see #getProxy()
     * 
     * @throws MatlabConnectionException
     * 
     * @param timeout time to wait in milliseconds for a proxy to be created
     * 
     * @return proxy
     */
    public MatlabProxy getProxy(long timeout) throws MatlabConnectionException;
    
    /**
     * Requests a {@link MatlabProxy}. Connection listeners will be notified upon successful creation. An exception will
     * be thrown if the factory has been shutdown.
     * <br><br>
     * The identifier of the proxy that will be created is returned. A proxy's identifier can be accessed by calling
     * {@link MatlabProxy#getIdentifier()}.
     * <br><br>
     * <strong>Running inside MATLAB</strong><br>
     * The proxy will be provided to the connection listeners shortly after this method returns.
     * <br><br>
     * <strong>Running outside MATLAB</strong><br>
     * This method will launch a new session of MATLAB. There is no timeout.
     * 
     * @see #addConnectionListener(MatlabConnectionListener)
     * @see MatlabProxy#getIdentifier()
     * @see #getProxy()
     * @see #getProxy(long)
     * 
     * @throws MatlabConnectionException
     * 
     * @return proxy's unique identifier
     */
    public String requestProxy() throws MatlabConnectionException;
    
    /**
     * Adds a listener to be notified when MATLAB connections are established and lost.
     * 
     * @param listener
     */
    public void addConnectionListener(MatlabConnectionListener listener);
    
    /**
     * Removes a listener so that it is no longer notified.
     * 
     * @param listener
     */
    public void removeConnectionListener(MatlabConnectionListener listener);
    
    /**
     * Cleanly shuts down the factory, allowing for the program to terminate properly. After this method has been
     * called, all calls to create proxies will fail. All proxies previously created by this factory will become
     * disconnected. Once a factory has been shutdown it cannot be turned back on.
     * <br><br>
     * This method may be safely called any number of times.
     * 
     * @throws MatlabConnectionException 
     */
    public void shutdown() throws MatlabConnectionException;
    
    /**
     * Whether the factory has been shutdown.
     * 
     * @return if shutdown
     */
    public boolean isShutdown();
}