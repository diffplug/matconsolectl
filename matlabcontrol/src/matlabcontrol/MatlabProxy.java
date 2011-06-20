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
 * Allows for Java to communicate with a running MATLAB session. This class cannot be instantiated, instead it can be
 * created by using a {@link MatlabProxyFactory}.
 * <br><br>
 * This proxy is thread-safe. While methods may be called concurrently, they will be completed sequentially on MATLAB's
 * main thread. More than one proxy may be interacting with MATLAB (for instance one proxy might be running inside
 * MATLAB and another might be running outside MATLAB) and their interactions will not occur simultaneously. Regardless
 * of the number of proxies, interaction with MATLAB occurs in a single threaded manner.
 * <br><br>
 * <strong>Running outside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>the proxy has been disconnected via {@link #disconnect()}</li>
 * <li>an internal MATLAB exception occurs*</li>
 * <li>communication between this JVM and the one MATLAB is running in is disrupted (likely due to closing MATLAB)</li>
 * <li>the class of the object to be returned is not {@link java.io.Serializable}</li>
 * <li>the class of the object to be sent or returned is not defined in the JVM receiving the object</li>
 * </ul>
 * <strong>Running inside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>the proxy has been disconnected via {@link #disconnect()}</li>
 * <li>an internal MATLAB exception occurs*</li>
 * <li>the method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components
 *     (This is done to prevent MATLAB from hanging indefinitely. To get around this limitation the
 *     {@link matlabcontrol.extensions.MatlabCallbackInteractor} can be used.)</li>
 * </ul>
 * *Internal MATLAB exceptions occur primarily for two reasons:
 * <ul>
 * <li>There is an error in the MATLAB code executing. An example of this includes attempting to call a function that
 *     does not exist (frequently due to a typo).</li>
 * <li>The Java MATLAB Interface (JMI) becoming out of sync with MATLAB. An example of this is an exception being thrown
 *     when attempting to retrieve or make use of a variable that has already been defined.</li>
 * </ul>
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public abstract class MatlabProxy implements MatlabInteractor<Object>
{
    /* Internal implementation notes:
     * 
     * This class could really be designed as an interface. It has intentionally not been. It is not an interface to
     * prevent users of the matlabcontrol API from making their own proxies, as being able to know for sure what the
     * MatlabProxy actually is, is a useful guarantee.
     */
    
    /**
     * This constructor is package private to prevent subclasses from outside of this package.
     */
    MatlabProxy() { }
    
    /**
     * Implementers can be notified of when the proxy becomes disconnected from MATLAB.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface DisconnectionListener
    {
        /**
         * Called when the proxy becomes disconnected from MATLAB. The proxy passed in will always be the proxy that
         * the listener was added to. The proxy is provided so that a single implementation of this interface may be
         * used for multiple proxies.
         * 
         * @param proxy disconnected proxy
         */
        public void proxyDisconnected(MatlabProxy proxy);
    }
    
    /**
     * Adds a disconnection that will be notified when this proxy becomes disconnected from MATLAB.
     * 
     * @param listener 
     */
    public abstract void addDisconnectionListener(DisconnectionListener listener);
    
    /**
     * Removes a disconnection listener. It will no longer be notified.
     * 
     * @param listener 
     */
    public abstract void removeDisconnectionListener(DisconnectionListener listener);
    
    /**
     * Whether this proxy is connected to MATLAB.
     * <br><br>
     * The most likely reasons for this method to return {@code false} is if MATLAB has been closed or it has been
     * disconnected via {@link #disconnect()}.
     * 
     * @return if connected
     */
    public abstract boolean isConnected();
    
    /**
     * Disconnects the proxy from MATLAB. MATLAB will not exit. After disconnecting, any method sent to MATLAB will
     * throw an exception. A proxy cannot be reconnected.
     * 
     * @return the success of disconnecting
     */
    public abstract boolean disconnect();
    
    /**
     * Returns the unique identifier for this proxy.
     * <br><br>
     * If this proxy was created by a call to {@link MatlabProxyFactory#requestProxy()}, then the {@code String} this
     * method returns matches that returned when calling to create this proxy.
     * 
     * @return identifier
     */
    public abstract String getIdentifier();
    
    /**
     * Whether this proxy is connected to a session of MATLAB that was running previous to the request to create this
     * proxy.
     * 
     * @return if existing session
     */
    public abstract boolean isExistingSession();
   
    //The following methods are overridden so that they appear in the javadocs

    @Override
    public abstract void eval(String command) throws MatlabInvocationException;

    @Override
    public abstract void exit() throws MatlabInvocationException;

    @Override
    public abstract void feval(String functionName, Object[] args) throws MatlabInvocationException;

    @Override
    public abstract Object getVariable(String variableName) throws MatlabInvocationException;

    @Override
    public abstract Object returningEval(String command, int returnCount) throws MatlabInvocationException;

    @Override
    public abstract Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException;

    @Override
    public abstract Object returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException;

    @Override
    public abstract void setDiagnosticMode(boolean enable) throws MatlabInvocationException;

    @Override
    public abstract void setVariable(String variableName, Object value) throws MatlabInvocationException;
    
    @Override
    public abstract String storeObject(Object obj, boolean storePermanently) throws MatlabInvocationException;
}