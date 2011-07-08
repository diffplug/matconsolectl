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

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Communicates with a running MATLAB session. This class cannot be instantiated, it may be created with a
 * {@link MatlabProxyFactory}. Interaction with MATLAB occurs as if calling {@code eval} and {@code feval} in the
 * MATLAB Command Window.
 * <h3>Communicating with MATLAB</h3>
 * Methods which interact with MATLAB provide Java objects to the MATLAB environment and retrieve data from the MATLAB
 * environment as Java objects. The following description of how the conversion between MATLAB and Java types occurs is
 * based on tests conducted on versions of MATLAB R2007b through R2010b. Unless otherwise noted the behavior was
 * identical on all tested versions. However, this behavior is not officially documented by The MathWorks and may change
 * for future versions of MATLAB. (matlabcontrol is not compatible with versions prior to MATLAB R2007b.)
 * <br><br>
 * <b>MATLAB to Java</b><br>
 * <br><i>Numeric Classes and Logical Class</i><br>
 * All MATLAB numeric types whether they are a singular value or an array/matrix (of any dimension) are always converted
 * into a one-dimensional Java {@code double[]}. For complex numbers, only the real component is converted. MATLAB
 * {@code logical}s whether a singular value or an array/matrix (of any dimension) are always converted into a
 * one-dimensional Java {@code boolean[]}. MATLAB arrays are stored in a linear manner, which has been
 * <a href="http://www.mathworks.com/help/techdoc/math/f1-85462.html#f1-85511">documented</a> by The MathWorks. It is in
 * this linear manner that MATLAB arrays are returned to Java. (Sparse matrices are stored differently and are not sent
 * to Java in an easy to use manner.)
 * <br><br><i>Character Class</i><br>
 * MATLAB {@code char} singular values are returned as a Java {@code String}. One-dimensional MATLAB {@code char}
 * arrays are returned as a Java {@code String}. Two-dimensional MATLAB {@code char} arrays are returned as a Java
 * {@code String[]} with each row of the MATLAB array becoming a Java {@code String}. MATLAB {@code char} arrays of
 * more than two dimensions have an inconsistent conversion to a Java type, although all observed conversions are
 * either a Java {@code String} or an array of {@code String}s.
 * <br><br><i>Cell and Struct Arrays</i><br>
 * MATLAB {@code cell} arrays and {@code struct} arrays are converted to a Java {@code Object[]}, often with arrays
 * inside of them.
 * <br><br><i>Function Handles and Non-Built-In Classes</i><br>
 * MATLAB {@code function_handle}s and all non-built-in classes (such as the {@code Map} class or user defined classes)
 * are converted to an instance of {@code com.mathworks.jmi.types.MLArrayRef} that is not {@code Serializable} which
 * prevents it from being transferred to a Java application running outside MATLAB (more information on this can be
 * found in the exception section below).
 * <br><br>
 * <b>Java to MATLAB</b><br>
 * <br><i>Primitives</i><br>
 * It is not possible to directly send a Java primitive to the MATLAB environment because all methods which interact
 * with MATLAB take in either {@code Object} or {@code Object[]} which results in the Java primitives becoming their
 * auto-boxed class equivalent. (For example {@code int} becomes {@code Integer}.) Java primitive arrays, such as
 * {@code int[]} are {@code Object}s and can therefore be sent to the MATLAB environment. They are converted as follows:
 * <br><br>
 * <center>
 * <table border="1" cellpadding="4">
 * <tr>   <th bgcolor="#C0C0C0">Java Type</th>   <th bgcolor="#C0C0C0">MATLAB Type</th> </tr>
 * <tr>   <td><code>boolean[]</code></td>        <td><code>logical</code> array</td>    </tr>
 * <tr>   <td><code>char[]</code></td>           <td>not supported*</td>                </tr>
 * <tr>   <td><code>byte[]</code></td>           <td><code>int8</code> array</td>       </tr>
 * <tr>   <td><code>short[]</code></td>          <td><code>int16</code> array</td>      </tr>
 * <tr>   <td><code>int[]</code></td>            <td><code>int32</code> array</td>      </tr>
 * <tr>   <td><code>long[]</code></td>           <td>not supported*</td>                </tr>
 * <tr>   <td><code>float[]</code></td>          <td><code>single</code> array</td>     </tr>
 * <tr>   <td><code>double[]</code></td>         <td><code>double</code> array</td>     </tr>
 * </table>
 * </center>
 * <br>
 * *In MATLAB R2009b and higher, MATLAB will throw an exception. In MATLAB R2008b and earlier, MATLAB will segfault. The
 * behavior in MATLAB R2009a is not known.
 * <br><br><i>{@code Number}s</i><br>
 * Subclasses of {@link Number}, which includes all of the auto-boxed versions of Java primitive numeric types, become
 * MATLAB {@code double}s.
 * <br><br><i>{@code Boolean}s</i><br>
 * {@link Boolean}s are converted to MATLAB {@code logical}s.
 * <br><br><i>{@code Character}s</i><br>
 * {@link Character}s are converted to MATLAB {@code char}s.
 * <br><br><i>{@code String}s</i><br>
 * {@link String}s are converted to MATLAB {@code char} arrays.
 * <br><br><i>Object Arrays</i><br>
 * Arrays of non-primitive types are converted to MATLAB {@code cell} arrays. The contents of the array are converted
 * according to these same rules. Note that Java's multidimensional arrays are not an exception to this rule. For
 * instance a {@code double[][]} is an array of {@code double[]}s and so MATLAB will create a cell array of MATLAB
 * {@code double} arrays.
 * <br><br><i>Other Classes</i><br>
 * Classes not otherwise mentioned remain as their original Java type. Objects contained within that class or instances
 * of that class are not automatically converted by MATLAB, although when fields or methods are accessed in the MATLAB
 * environment they may be converted into MATLAB types as
 * <a href="http://www.mathworks.com/help/techdoc/matlab_external/f6671.html">documented</a> by The MathWorks.
 * <br><br>
 * <b>Behavior of transferred data</b><br>
 * How Java objects sent to MATLAB or retrieved from MATLAB behave depends on several factors:
 * <br><br>
 * <i>Running outside MATLAB</i><br>
 * References to Java objects are copies. (There is one exception to this rule. Objects that are {@link java.rmi.Remote}
 * will act as if they are not copies. This is because matlabcontrol communicates with MATLAB's Java Virtual Machine
 * using
 * <a href="http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136424.html">Remote Method Invocation</a>.)
 * <br><br>
 * <i>Running inside MATLAB</i><br>
 * References to Java objects in MATLAB that are returned to Java, reference the same object. When passing a reference
 * to a Java object to MATLAB, if the Java object is not converted to a MATLAB type then it will reference the same
 * object in the MATLAB environment.
 * <br><br>
 * <b>Help transferring data</b><br>
 * The {@link matlabcontrol.extensions.MatlabProxyLogger} exists to record what is being returned from MATLAB. 
 * The {@link matlabcontrol.extensions.MatlabTypeConverter} can convert between complicated Java and MATLAB types.
 * Currently only MATLAB numeric arrays are supported.
 * <h3>Exceptions</h3>
 * Proxy methods that are relayed to MATLAB can throw {@link MatlabInvocationException}s. They will be thrown if:
 * <ul>
 * <li>An internal MATLAB exception occurs. This occurs primarily for two different reasons. The first is anything that
 *     would normally cause an error in MATLAB such as trying to use a function improperly or referencing a variable
 *     that does not exist. The second is due to the undocumented nature of the underlying Java MATLAB Interface API,
 *     such as trying to send a {@code long[]} to MATLAB.</li>
 * <li>The proxy has been disconnected via {@link #disconnect()}.</li>
 * <br><i>Running outside MATLAB</i>
 * <li>Communication between this Java Virtual Machine and the one that MATLAB is running in is disrupted (likely due to
 *     closing MATLAB).</li>
 * <li>The class of an object to be sent or returned is not {@link java.io.Serializable} or {@link java.rmi.Remote}.
 *     <sup>1</sup> Java primitives and arrays behave as if they were {@code Serializable}.</li>
 * <li>The class of an object to be returned from MATLAB is not defined in your application and no
 *     {@link SecurityManager} has been installed.<sup>2</sup></li>
 * <li>The class of an object to sent to MATLAB is not defined in MATLAB and the class is not on your application's
 *     classpath.<sup>3</sup></li>
 * <br><i>Running inside MATLAB</i>
 * <li>The method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components.<sup>4</sup> (A
 *     {@link matlabcontrol.extensions.CallbackMatlabProxy} may be used to interact with MATLAB on the EDT.) This
 *     does not apply to {@link #exit()} which may be called from the EDT.</li>
 * </ul>
 * <sup>1</sup>This is a requirement of Remote Method Invocation, which matlabcontrol uses when running outside MATLAB.
 * <br><br>
 * <sup>2</sup> This is due to Remote Method Invocation prohibiting loading classes defined in remote Java Virtual
 * Machines unless a {@code SecurityManager} has been set. {@link PermissiveSecurityManager} exists to provide an easy
 * way to set a security manager without further restricting permissions. Please consult
 * {@code PermissiveSecurityManager}'s documentation for more information.
 * <br><br>
 * <sup>3</sup> MATLAB sessions started by a {@code MatlabProxyFactory} are able to load all classes defined in your
 * application's class path as specified by the {@code java.class.path} property. Some frameworks load classes without
 * placing them on the class path, in that case matlabcontrol will not know about them and cannot tell MATLAB how to
 * load them.
 * <br><br>
 * <sup>4</sup> This is done to prevent MATLAB from hanging indefinitely. When interacting with MATLAB the calling
 * thread (unless it is the main MATLAB thread) is paused until MATLAB completes the requested operation. When a thread
 * is paused, no work can be done on the thread. MATLAB makes extensive use of the EDT when creating or manipulating
 * figure windows, uicontrols, plots, and other graphical elements. For instance, calling {@code plot} from the EDT
 * would never return because the {@code plot} function waits for the EDT to dispatch its event, which will never occur,
 * because the thread has been paused. A related, but far less critical issue, is that pausing the EDT would make the
 * user interface of MATLAB and any other Java GUI code running inside MATLAB non-responsive until MATLAB completed
 * evaluating the command.
 * <h3>Thread Safety</h3>
 * This proxy is unconditionally thread-safe. Methods which interact with MATLAB may be called concurrently; however
 * they will be completed sequentially on MATLAB's main thread. Calls to MATLAB from a given thread will be executed in
 * the order they were invoked. No guarantees are made about the relative ordering of calls made from different threads.
 * This proxy may not be the only thing interacting with MATLAB's main thread. One proxy running outside MATLAB and any
 * number of proxies running inside MATLAB may be simultaneously connected. If MATLAB is not hidden from user
 * interaction then a user may also be making use of MATLAB's main thread. This means that two sequential calls to the
 * proxy from the same thread that interact with MATLAB will execute in that order, but interactions with MATLAB may
 * occur between the two calls. In typical use it is unlikely this behavior will pose a problem. However, for some uses
 * cases it may be necessary to guarantee that several interactions with MATLAB occur without interruption.
 * Uninterrupted access to MATLAB's main thread may be obtained by use of
 * {@link #invokeAndWait(MatlabProxy.MatlabThreadCallable) invokeAndWait(...)}.
 * <h3>Threads</h3>
 * When <i>running outside MATLAB</i>, the proxy makes use of multiple internally managed threads. When the proxy
 * becomes disconnected from MATLAB it notifies its disconnection listeners and then terminates all threads it was using
 * internally. A proxy may disconnect from MATLAB without exiting MATLAB by calling {@link #disconnect()}.
 * 
 * @see MatlabProxyFactory#getProxy()
 * @see MatlabProxyFactory#requestProxy(matlabcontrol.MatlabProxyFactory.RequestCallback)
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public abstract class MatlabProxy implements MatlabInteractor
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
                " identifier=" + this.getIdentifier() + "," +
                " connected=" + this.isConnected() + "," +
                " insideMatlab=" + this.isRunningInsideMatlab() + "," +
                " existingSession=" + this.isExistingSession() +
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
     * Whether this proxy is running inside of MATLAB.
     * 
     * @return
     */
    public abstract boolean isRunningInsideMatlab();
    
    /**
     * Whether this proxy is connected to MATLAB.
     * <br><br>
     * The most likely reasons for this method to return {@code false} if the proxy has been disconnected via
     * {@link #disconnect()} or is if MATLAB has been closed (when running outside MATLAB).
     * 
     * @return if connected
     * 
     * @see #disconnect() 
     * @see #exit()
     */
    public abstract boolean isConnected();
    
    /**
     * Disconnects the proxy from MATLAB. MATLAB will not exit. After disconnecting, any method sent to MATLAB will
     * throw an exception. A proxy cannot be reconnected. Returns {@code true} if the proxy is now disconnected,
     * {@code false} otherwise.
     * 
     * @return if disconnected
     * 
     * @see #exit()
     * @see #isConnected() 
     */
    public abstract boolean disconnect();
    
    /**
     * Exits MATLAB. Attempting to exit MATLAB with either a {@code eval} or {@code feval} command will cause MATLAB to
     * hang indefinitely.
     * 
     * @throws MatlabInvocationException 
     * 
     * @see #disconnect()
     * @see #isConnected() 
     */
    public abstract void exit() throws MatlabInvocationException;
    
    /**
     * Runs the {@code callable} on MATLAB's main thread and waits for it to return its result. This method allows for
     * uninterrupted access to MATLAB's main thread between two or more interactions with MATLAB.
     * <br><br>
     * If <i>running outside MATLAB</i> the {@code callable} must be {@link java.io.Serializable}; it may not be
     * {@link java.rmi.Remote}.
     * 
     * @param <T>
     * @param callable
     * @return result of the callable
     * @throws MatlabInvocationException 
     */
    public abstract <T> T invokeAndWait(MatlabThreadCallable<T> callable) throws MatlabInvocationException;
    
    /**
     * Uninterrupted block of computation performed in MATLAB.
     * 
     * @see MatlabProxy#invokeAndWait(matlabcontrol.MatlabProxy.MatlabThreadCallable) 
     * @param <T> type of the data returned by the callable
     */
    public static interface MatlabThreadCallable<T>
    {
        /**
         * Performs the computation in MATLAB. The {@code proxy} provided will invoke its methods directly on MATLAB's
         * main thread without delay. This {@code proxy} should be used to interact with MATLAB, not a
         * {@code MatlabProxy} (or any class delegating to it).
         * 
         * @param proxy
         * @return result of the computation
         * @throws MatlabInvocationException
         */
        public T call(MatlabThreadProxy proxy) throws MatlabInvocationException;
    }
    
    /**
     * Operates on MATLAB's main thread without interruption. This interface is not intended to be implemented by users
     * of matlabcontrol.
     * <br><br>
     * An implementation of this interface is provided to
     * {@link MatlabThreadCallable#call(MatlabProxy.MatlabThreadProxy)} so that the callable can interact with
     * MATLAB. Implementations of this interface behave identically to a {@link MatlabProxy} running inside of MATLAB
     * except that they are <b>not</b> thread-safe. They must be used solely on the thread that calls
     * {@link MatlabThreadCallable#call(MatlabProxy.MatlabThreadProxy) call(...)}.
     */
    public static interface MatlabThreadProxy extends MatlabInteractor
    {
        
    }
    
    /**
     * Listens for a proxy's disconnection from MATLAB.
     * 
     * @see MatlabProxy#addDisconnectionListener(matlabcontrol.MatlabProxy.DisconnectionListener)
     * @see MatlabProxy#removeDisconnectionListener(matlabcontrol.MatlabProxy.DisconnectionListener) 
     * @since 4.0.0
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface DisconnectionListener
    {
        /**
         * Called when the proxy becomes disconnected from MATLAB. The proxy passed in will always be the proxy that
         * the listener was added to. The proxy is provided so that if desired a single implementation of this
         * interface may easily be used for multiple proxies.
         * 
         * @param proxy disconnected proxy
         */
        public void proxyDisconnected(MatlabProxy proxy);
    }
    
    /**
     * Uniquely identifies a proxy. This interface is not intended to be implemented by users of matlabcontrol.
     * <br><br>
     * Implementations of this interface are unconditionally thread-safe.
     * 
     * @since 4.0.0
     * 
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface Identifier
    {
        /**
         * Returns {@code true} if {@code other} is equal to this identifier, {@code false} otherwise.
         * 
         * @param other
         * @return 
         */
        @Override
        public boolean equals(Object other);
        
        /**
         * Returns a hash code which conforms to the {@code hashCode} contract defined in {@link Object#hashCode()}.
         * 
         * @return 
         */
        @Override
        public int hashCode();
    }
}