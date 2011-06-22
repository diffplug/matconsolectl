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

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Allows for Java to communicate with a running MATLAB session. This class cannot be instantiated, it may be created
 * with a {@link MatlabProxyFactory}. The primary methods used to communicate with MATLAB are defined in the
 * {@link MatlabInteractor} interface which this class implements. Interaction with MATLAB occurs as if calling
 * {@code eval} and {@code feval} in the MATLAB Command Window.
 * <h3>Communicating with MATLAB</h3>
 * Methods which interact with MATLAB may provide any objects as function arguments and those methods return any
 * object. (When running outside MATLAB there are further restrictions, documented below.) As such the return type of
 * all methods that interact with MATLAB and return a value is {@code Object}. Primitives will be autoboxed
 * appropriately. Certain Java types will be converted into MATLAB types as
 * <a href="http://www.mathworks.com/help/techdoc/matlab_external/f6671.html">documented</a> by MathWorks. Similarly
 * MATLAB types are converted into Java types as
 * <a href="http://www.mathworks.com/help/techdoc/matlab_external/f6425.html">documented</a> by MathWorks. The
 * documentation provides the information from the perspective of MATLAB code calling Java code, which is officially
 * supported. For matlabcontrol, the <i>opposite</i> is occurring with Java calling MATLAB code.
 * <br><br>
 * The {@link matlabcontrol.extensions.DiagnosticMatlabInteractor} exists to help determine what is being returned by
 * MATLAB. {@link matlabcontrol.extensions.ReturnDataMatlabInteractor} simplifies casting the returned data. MATLAB
 * matrices are implemented in a fundamentally different manner than Java arrays.
 * {@link matlabcontrol.extensions.MatlabMatrix} can convert between the two formats; MATLAB matrices can be sent to and
 * retrieved from MATLAB with the {@link matlabcontrol.extensions.MatrixProcessor}. Some of these extensions implement
 * {@code MatlabInteractor} and all operate on {@code MatlabInteractor}. They can therefore be combined with one
 * another. You may wish to develop your own wrapper around this proxy, if you do so should consider operating on a
 * {@code MatlabInteractor} instead of a {@code MatlabProxy} so that your code can be used with other extensions. 
 * <br><br>
 * How Java objects sent to MATLAB or retrieved from MATLAB behave depends on several factors:
 * <br><br>
 * <strong>Running inside MATLAB</strong><br>
 * References to Java objects in MATLAB that are returned to Java, reference the same object. When passing a reference
 * to a Java object to MATLAB, if the Java object is <i>not</i> converted to a MATLAB type then it will reference the
 * same object in the MATLAB environment.
 * <br><br>
 * <strong>Running outside MATLAB</strong><br>
 * References to Java objects are copies. There is one exception to this rule. Objects that are
 * {@link java.rmi.Remote} will act as if they are not copies. This is because matlabcontrol communicates with MATLAB's
 * Java Virtual Machine using <a href="http://download.oracle.com/javase/6/docs/platform/rmi/spec/rmiTOC.html">Remote
 * Method Invocation</a>. Properly using RMI is non-trivial, if you plan to make use of {@code Remote} objects you
 * should take care to understand how RMI operates.
 * <h3>Thread Safety</h3>
 * This proxy is thread-safe. Methods defined in {@code MatlabInteractor} as well as {@link #exit()} and 
 * {@link #invokeAndWait(matlabcontrol.MatlabProxy.MatlabThreadCallable) invokeAndWait(...)} may be called concurrently;
 * however they will be completed sequentially on MATLAB's main thread. Calls to MATLAB from a given thread will be
 * executed in the order they were invoked. No guarantees are made about the relative ordering of calls made from
 * different threads. This proxy may not be the only thing interacting with MATLAB's main thread. One proxy running
 * outside MATLAB and any number of proxies running inside MATLAB may be simultaneously connected. If MATLAB is not
 * hidden from user interaction then a user may also be making use of MATLAB's main thread. This means that two
 * sequential calls to the proxy from the same thread that interact with MATLAB will execute in that order, but
 * interactions with MATLAB may occur between the two calls. In typical use this behavior will not pose a problem.
 * However, in some multi-threaded uses cases it may be necessary to guarantee that several interactions with MATLAB
 * occur without interruption. Uninterrupted access to MATLAB's main thread may be obtained by use of
 * {@link #invokeAndWait(matlabcontrol.MatlabProxy.MatlabThreadCallable) invokeAndWait(...)}.
 * <h3>Threads</h3>
 * When <strong>running outside MATLAB</strong>, the proxy makes use of multiple internally managed threads. When the
 * proxy becomes disconnected from MATLAB it notifies its disconnection listeners and then terminates all threads it was
 * using internally.
 * <h3>Exceptions</h3>
 * Proxy methods that are relayed to MATLAB can throw {@link MatlabInvocationException}s. They will be thrown if:
 * <ul>
 * <li>An internal MATLAB exception occurs, typically from trying to use a function or variable that does not exist, or
 *     incorrectly calling a function (such as providing the wrong number of arguments).</li>
 * <li>The proxy has been disconnected via {@link #disconnect()}.</li>
 * <br><strong>Running outside MATLAB</strong>
 * <li>Communication between this JVM and the one that MATLAB is running in is disrupted (likely due to closing
 *     MATLAB).</li>
 * <li>The class of the object to be sent or returned is not {@link java.io.Serializable} (unless it is {@code Remote}.
 *     Java primitives and arrays are {@code Serializable}.</li>
 * <li>The class of the object to be sent or returned is not defined in the Java Virtual Machine receiving the
 *     object.*</li>
 * <br><strong>Running inside MATLAB</strong>
 * <li>The method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components.✝ (To get around
 *     this limitation a {@link matlabcontrol.extensions.MatlabCallbackInteractor} can be used.)</li>
 * </ul>
 * * This limitation is due to Java prohibiting loading arbitrary classes from remote Java Virtual Machines unless a
 * {@link SecurityManager} has been set that will allow this behavior. matlabcontrol intentionally does not set a
 * {@code SecurityManager}, but you may do so. A {@link java.rmi.RMISecurityManager} is configured to allow this
 * behavior. You must set the security manager before constructing a {@code MatlabProxyFactory}.
 * <br><br>
 * ✝ This is done to prevent MATLAB from hanging indefinitely. In order to properly interact with MATLAB the calling
 * thread (unless it is the main MATLAB thread) is paused until MATLAB completes the requested operation. When a thread
 * is paused, no work can be done on the thread. MATLAB makes extensive use of the EDT when creating or manipulating
 * figure windows, uicontrols or plots. For instance, calling {@code plot} from the EDT would never  return because the
 * {@code plot} function waits for the EDT to dispatch its event, which will never occur, because the thread has been
 * paused. A related, but far less critical issue, is that pausing the EDT would make user interface of MATLAB and any
 * other Java GUI code running inside MATLAB non-responsive until MATLAB completed evaluating the command.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public abstract class MatlabProxy implements MatlabInteractor<Object>
{
    /**
     * Unique identifier for this proxy.
     */
    private final Identifier _id;
    
    /**
     * Whether the session of MATLAB this proxy is connected to is an existing session.
     */
    private final boolean _existingSession;
    
    /**
     * Listeners for disconnection.
     */
    private final CopyOnWriteArrayList<DisconnectionListener> _listeners;
    
    /**
     * This constructor is package private to prevent subclasses from outside of this package.
     */
    MatlabProxy(Identifier id, boolean existingSession)
    {
        _id = id;
        _existingSession = existingSession;
        
        _listeners = new CopyOnWriteArrayList<DisconnectionListener>();
    }
    
    /**
     * Returns the unique identifier for this proxy.
     * 
     * @return identifier
     */
    public Identifier getIdentifier()
    {
        return _id;
    }
        
    /**
     * Whether this proxy is connected to a session of MATLAB that was running previous to the request to create this
     * proxy.
     * 
     * @return if existing session
     */
    public boolean isExistingSession()
    {
        return _existingSession;
    }
    
    /**
     * Returns a brief description of this proxy. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() +
                " identifier=" + this.getIdentifier() + ", " +
                " connected=" + this.isConnected() + ", " +
                " existing=" + this.isExistingSession() + 
                "]";
    }
    
    /**
     * Adds a disconnection that will be notified when this proxy becomes disconnected from MATLAB.
     * 
     * @param listener 
     */
    public void addDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.add(listener);
    }

    /**
     * Removes a disconnection listener. It will no longer be notified.
     * 
     * @param listener 
     */
    public void removeDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.remove(listener);
    }
    
    /**
     * Notifies the disconnection listeners this proxy has become disconnected.
     */
    void notifyDisconnectionListeners()
    {
        for(DisconnectionListener listener : _listeners)
        {
            listener.proxyDisconnected(this);
        }
    }
    
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
     * throw an exception. A proxy cannot be reconnected. Returns {@code true} if the proxy is now disconnected.
     * 
     * @return if disconnected
     */
    public abstract boolean disconnect();
    
    /**
     * Exits MATLAB. Attempting to exit MATLAB with either a {@code eval} or {@code feval} command will cause MATLAB to
     * hang indefinitely.
     * 
     * @throws MatlabInvocationException 
     */
    public abstract void exit() throws MatlabInvocationException;
    
    /**
     * TODO: DOCUMENT ME!
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws MatlabInvocationException 
     */
    public abstract <T> T invokeAndWait(MatlabThreadCallable<T> callable) throws MatlabInvocationException;
    
    /**
     * Implementers can be notified when a proxy becomes disconnected from MATLAB.
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
     * Uniquely identifies a proxy.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface Identifier
    {
        /**
         * Returns {@code true} if {@code other} is an identifier and is equal to this identifier, {@code false}
         * otherwise.
         * 
         * @param other
         * @return 
         */
        @Override
        public boolean equals(Object other);
    }
    
    /**
     * TODO: DOCUMENT ME
     * 
     * @param <T> 
     */
    public static interface MatlabThreadCallable<T>
    {
        /**
         * TODO: DOCUMENT ME
         * 
         * @param proxy
         * @return
         * @throws Exception 
         */
        public T call(MatlabInteractor<Object> proxy) throws Exception;
    }
}