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
 * with a {@link MatlabProxyFactory}. Interaction with MATLAB occurs as if calling {@code eval} and {@code feval} in the
 * MATLAB Command Window.
 * <h3>Communicating with MATLAB</h3>
 * Methods which interact with MATLAB may provide any objects as function arguments (with a few exceptions documented in
 * the next section) and those methods return any object. (When running outside MATLAB there are further restrictions,
 * documented in the exceptions section.) As such all methods that send data to MATLAB take in either {@code Object} or
 * {@code Object[]}, and the return type of all methods that interact with MATLAB is {@code Object}. Primitives will be
 * auto-boxed as necessary.
 * <br><br>
 * <b>MATLAB to Java</b><br>
 * When values are returned from MATLAB they will be converted to Java types as needed. MATLAB fully supports calling
 * Java from MATLAB which can result in converting MATLAB types to Java types. MathWorks has
 * <a href="http://www.hulu.com/watch/13827/saturday-night-live-snl-digital-short-andys-dad">documented</a> this
 * behavior. This proxy causes MATLAB values to be returned to Java as opposed to becoming a method's arguments. The
 * behavior of this is entirely undocumented and is not entirely consistent with how MATLAB types are converted when
 * provided to a Java method. How they are converted may differ between versions.  The following descriptions are
 * what occurs when using MATLAB R2010b.
 * <br><br>
 * If the value is a Java type, it will not be converted. MATLAB numeric types {@code double}, {@code single},
 * {@code int8}, {@code uint8}, {@code int16}, {@code uint16}, {@code int32}, {@code uint32}, {@code int64}, and
 * {@code uint64} are all converted to the Java {@code double} type. The MATLAB {@code logic} type is converted to the
 * Java {@code boolean} type. The MATLAB types just mentioned are always MATLAB arrays even if they appear as singular
 * values and as such are always returned to Java as either a {@code double} array or {@code boolean} array. MATLAB
 * {@code char} arrays are converted to Java {@code String}s. MATLAB {@code cell} arrays and {@code struct}s are
 * converted to Java {@code Object} arrays, which may contain arrays as their elements. MATLAB {@code function_handle}s
 * and user-defined classes are returned as the Java type {@code com.mathworks.jmi.types.MLArrayRef} which is not
 * {@code Serializable} and therefore cannot be returned to a Java application running outside of MATLAB (see exceptions
 * section for more information).
 * <br><br>
 * <b>Java to MATLAB</b><br>
 * When Java values are sent to MATLAB they may be converted to MATLAB types. MATLAB fully supports Java methods
 * returning Java values into the MATLAB environment. MathWorks has
 * <a href="http://www.mathworks.com/help/techdoc/matlab_external/f6671.html">documented</a> this behavior. This proxy
 * causes Java values to be provided to MATLAB functions. The behavior of this is undocumented and is not entirely
 * consistent with how Java types are converted to MATLAB types when returned from Java method. How they are converted
 * may differ between versions. The following descriptions are what occurs when using MATLAB R2010b.
 * <br><br><i>Truth values</i><br>
 * Java's {@code boolean} primitive and {@code Boolean} class are both converted to a MATLAB {@code logical} A Java
 * {@code boolean[]} becomes a MATLAB {@code logical} array. A Java {@code Boolean[]} is not converted.
 * <br><br><i>Text</i><br>
 * Java's {@code char} primitive and {@code Character} class are both converted to a MATLAB {@code char}. A Java
 * {@code char[]} cannot be directly sent to MATLAB, doing so results in an internal MATLAB exception. If the
 * {@code char[]} is encapsulated in a Java class that is not an array, it may be sent to MATLAB, which if accessed from
 * MATLAB will be converted to a MATLAB {@code char} array. A Java {@code Character[]} is not converted. A Java
 * {@code String} is converted to a MATLAB {@code char} array. A Java {@code String[]} is treated no differently than
 * other Java arrays, see the <i>arrays</i> section below.
 * <br><br><i>Numbers</i><br>
 * Java's {@code double}, {@code float}, {@code long}, {@code int}, {@code short}, and {@code byte} primitives are all
 * converted to MATLAB's {@code double} type. Any {@link Number}, including the auto-boxed versions of the just
 * mentioned numeric primitives will be converted to a MATLAB {@code double}. Single dimension arrays of Java primitive
 * numeric values are handled differently. A Java {@code double[]} will become a MATLAB {@code double} array. A Java
 * {@code float[]} will become a MATLAB {@code single} array. A Java {@code int[]} will become a MATLAB {@code int32}
 * array. A Java {@code byte[]} will become a MATLAB {@code int8} array. Java's {@code long[]} and {@code short[]}
 * cannot be directly sent to MATLAB, doing so results in an internal MATLAB exception. If either is encapsulated in a
 * Java class that is not an array it be may be sent to MATLAB, which if accessed from MATLAB will become a MATLAB
 * {@code int64} array and a MATLAB {@code int16} array respectively. Java's {@code Double[]}, {@code Float[]},
 * {@code Long[]}, {@code Integer[]}, {@code Short[]}, and {@code Byte[]} are not converted.
 * <br><br><i>Arrays</i><br>
 * With the exception of the Java arrays mentioned above, Java arrays are converted to MATLAB cell arrays. The
 * elements of the array are in turn converted in the manner as described in this Java to MATLAB section. Note that
 * Java's multidimensional arrays are not an exception to this rule. For instance a {@code double[][]} is an array of
 * {@code double[]}s and so MATLAB will create a cell array of MATLAB {@code double} arrays.
 * <br><br><i>Other Java {@code Object}s</i><br>
 * Java {@code Object}s not mentioned above will remain Java {@code Object}s, they will not be converted into MATLAB
 * types.
 * <br><br>
 * <b>Behavior of transferred data</b><br>
 * How Java objects sent to MATLAB or retrieved from MATLAB behave depends on several factors:
 * <br><br>
 * <i>Running outside MATLAB</i><br>
 * References to Java objects are copies. (There is one exception to this rule. Objects that are {@link java.rmi.Remote}
 * will act as if they are not copies. This is because matlabcontrol communicates with MATLAB's Java Virtual Machine
 * using <a href="http://download.oracle.com/javase/6/docs/platform/rmi/spec/rmiTOC.html">Remote Method Invocation</a>.
 * Properly using RMI is non-trivial, if you plan to make use of {@code Remote} objects you should take care to
 * understand how RMI operates.)
 * <br><br>
 * <i>Running inside MATLAB</i><br>
 * References to Java objects in MATLAB that are returned to Java, reference the same object. When passing a reference
 * to a Java object to MATLAB, if the Java object is <i>not</i> converted to a MATLAB type then it will reference the
 * same object in the MATLAB environment.
 * <br><br>
 * <b>Help transferring data</b><br>
 * The {@link matlabcontrol.extensions.LoggingMatlabInteractor} exists to record what is being returned from MATLAB. To
 * easily transfer between MATLAB matrices and Java multi-dimensional arrays a
 * {@link matlabcontrol.extensions.MatlabMatrix} may be used. These matrices can be sent to and retrieved from MATLAB
 * with a {@link matlabcontrol.extensions.MatrixProcessor}. {@link matlabcontrol.extensions.ReturnDataMatlabInteractor}
 * simplifies casting the returned data.
 * <h3>Thread Safety</h3>
 * This proxy is unconditionally thread-safe. Methods defined in {@link MatlabInteractor} as well as {@link #exit()} may
 * be called concurrently; however they will be completed sequentially on MATLAB's main thread. Calls to MATLAB from a
 * given thread will be executed in the order they were invoked. No guarantees are made about the relative ordering of
 * calls made from different threads. This proxy may not be the only thing interacting with MATLAB's main thread. One
 * proxy running outside MATLAB and any number of proxies running inside MATLAB may be simultaneously connected. If
 * MATLAB is not hidden from user interaction then a user may also be making use of MATLAB's main thread. This means
 * that two sequential calls to the proxy from the same thread that interact with MATLAB will execute in that order, but
 * interactions with MATLAB may occur between the two calls. In typical use it is unlikely this behavior will pose a
 * problem. However, for some uses cases it may be necessary to guarantee that several interactions with MATLAB occur
 * without interruption. Uninterrupted access to MATLAB's main thread may be obtained by use of
 * {@link #invokeAndWait(matlabcontrol.MatlabInteractor.MatlabCallable) invokeAndWait(...)}.
 * <h3>Threads</h3>
 * When <strong>running outside MATLAB</strong>, the proxy makes use of multiple internally managed threads. When the
 * proxy becomes disconnected from MATLAB it notifies its disconnection listeners and then terminates all threads it was
 * using internally. A proxy may disconnect from MATLAB without exiting MATLAB by calling {@link #disconnect()}.
 * <h3>Exceptions</h3>
 * Proxy methods that are relayed to MATLAB can throw {@link MatlabInvocationException}s. They will be thrown if:
 * <ul>
 * <li>An internal MATLAB exception occurs, typically from trying to use a function or variable that does not exist, or
 *     incorrectly calling a function (such as providing the wrong number of arguments).</li>
 * <li>The proxy has been disconnected via {@link #disconnect()}.</li>
 * <br><i>Running outside MATLAB</i>
 * <li>Communication between this Java Virtual Machine and the one that MATLAB is running in is disrupted (likely due to
 *     closing MATLAB).</li>
 * <li>The class of the object to be sent or returned is not {@link java.io.Serializable} or {@link java.rmi.Remote}.
 *     Java primitives and arrays behave as if they were {@code Serializable}.</li>
 * <li>The class of the object to be returned from MATLAB is not defined in your application and no
 *     {@link SecurityManager} has been installed.*</li>
 * <li>The class of the object to sent to MATLAB is not defined in MATLAB and the class is not on your application's
 *     classpath.⊤</li>
 * <br><i>Running inside MATLAB</i>
 * <li>The method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components.✝ (A
 *     {@link matlabcontrol.extensions.MatlabCallbackInteractor} may be used to interact with MATLAB on the EDT.) This
 *     does not apply to {@link #exit()} which may be called from the EDT.</li>
 * </ul>
 * * This is due to Remote Method Invocation (RMI) prohibiting loading classes defined in remote Java Virtual Machines
 * unless a {@code SecurityManager} has been set. {@link PermissiveSecurityManager} exists to provide an easy way to set
 * a security manager without further restricting permissions. Please consult {@code PermissiveSecurityManager}'s
 * documentation for more information.
 * <br><br>
 * ⊤ MATLAB sessions started by a {@code MatlabProxyFactory} are able to load all classes defined in your application's
 * class path as specified by the {@code java.class.path} property. Some frameworks load classes without placing them
 * on the class path, in that case matlabcontrol will not know about them and cannot tell MATLAB how to load them.
 * <br><br>
 * ✝ This is done to prevent MATLAB from hanging indefinitely. When interacting with MATLAB the calling thread (unless
 * it is the main MATLAB thread) is paused until MATLAB completes the requested operation. When a thread is paused, no
 * work can be done on the thread. MATLAB makes extensive use of the EDT when creating or manipulating figure windows,
 * uicontrols, plots, and other graphical elements. For instance, calling {@code plot} from the EDT would never  return
 * because the {@code plot} function waits for the EDT to dispatch its event, which will never occur, because the thread
 * has been paused. A related, but far less critical issue, is that pausing the EDT would make the user interface of
 * MATLAB and any other Java GUI code running inside MATLAB non-responsive until MATLAB completed evaluating the
 * command.
 * 
 * @see MatlabProxyFactory#getProxy()
 * @see MatlabProxyFactory#requestProxy(matlabcontrol.MatlabProxyFactory.RequestCallback)
 * @since 4.0.0
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
     * The most likely reasons for this method to return {@code false} if the proxy has been disconnected via
     * {@link #disconnect()} or is if MATLAB has been closed (when running outside MATLAB).
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
     * Evaluates a command in MATLAB. This is equivalent to MATLAB's {@code eval('command')}.
     * 
     * @param command the command to be evaluated in MATLAB
     * @throws MatlabInvocationException 
     * @see #returningEval(String, int)
     */
    @Override
    public abstract void eval(String command) throws MatlabInvocationException;

    /**
     * Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's {@code eval('command')}.
     * <br><br>
     * In order for the result of this command to be returned the number of arguments to be returned must be specified
     * by {@code returnCount}. If the command you are evaluating is a MATLAB function you can determine the amount of
     * arguments it returns by using the {@code nargout} function in the MATLAB Command Window. If it returns
     * {@code -1} that means the function returns a variable number of arguments based on what you pass in. In that
     * case, you will need to manually determine the number of arguments returned. If the number of arguments returned
     * differs from {@code returnCount} then either {@code null} or an empty {@code String} will be returned.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param returnCount the number of arguments that will be returned from evaluating the command
     * @see #eval(String)
     * @return result of MATLAB {@code eval}
     * @throws MatlabInvocationException 
     */
    @Override
    public abstract Object returningEval(String command, int returnCount) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}. Arguments to the function may be provided as
     * {@code args}, if you wish to call the function with no arguments pass in {@code null}. The result of this
     * function will not be returned.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @throws MatlabInvocationException 
     * @see #returningFeval(String, Object[], int)
     * @see #returningFeval(String, Object[])
     */
    @Override
    public abstract void feval(String functionName, Object[] args) throws MatlabInvocationException;

    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * <br><br>
     * The result of this function can be returned. In order for a function's return data to be returned to MATLAB it is
     * necessary to know how many arguments will be returned. This method will attempt to determine that automatically,
     * but in the case where a function has a variable number of arguments an exception will be thrown. To invoke the
     * function successfully use {@link #returningFeval(String, Object[], int)} and specify the number of arguments
     * that will be returned for the provided arguments.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     */
    @Override
    public abstract Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * <br><br>
     * The result of this function can be returned. In order for the result of this function to be returned the number
     * of arguments to be returned must be specified by {@code returnCount}. You can use the {@code nargout} function in
     * the MATLAB Command Window to determine the number of arguments that will be returned. If {@code nargout} returns
     * {@code -1} that means the function returns a variable number of arguments based on what you pass in. In that
     * case, you will need to manually determine the number of arguments returned. If the number of arguments returned
     * differs from {@code returnCount} then either only some of the items will be returned or {@code null} will be
     * returned.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @param returnCount the number of arguments that will be returned from this function
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     */
    @Override
    public abstract Object returningFeval(String functionName, Object[] args, int returnCount)
            throws MatlabInvocationException;
    
    /**
     * Sets {@code variableName} to {@code value} in MATLAB, creating the variable if it does not yet exist.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException
     */
    @Override
    public abstract void setVariable(String variableName, Object value) throws MatlabInvocationException;
    
    /**
     * Gets the value of {@code variableName} in MATLAB.
     * 
     * @param variableName
     * @return value
     * @throws MatlabInvocationException
     */
    @Override
    public abstract Object getVariable(String variableName) throws MatlabInvocationException;
    
    /**
     * Runs the {@code callable} on MATLAB's main thread and waits for it to return its result. This method allows for
     * uninterrupted access to MATLAB's main thread between two or more interactions with MATLAB.
     * <br><br>
     * The {@link MatlabInteractor} provided to the {@code callable} will invoke its methods directly on MATLAB's main
     * thread without delay. The interactor will behave identically to a {@code MatlabProxy} running inside MATLAB which
     * is being used on MATLAB's main thread. This interactor should be used to interact with MATLAB, not a
     * {@code MatlabProxy} (or any class delegating to it).
     * <br><br>
     * All restrictions that apply to arguments passed to other methods that interact with MATLAB also apply to this
     * method. In particular, this means that if <i>running outside MATLAB</i> the {@code callable} must be either
     * {@link java.io.Serializable} or {@link java.rmi.Remote}.
     * 
     * @param <T>
     * @param callable
     * @return result of the callable
     * @throws MatlabInvocationException 
     */
    @Override
    public abstract <T> T invokeAndWait(MatlabCallable<T> callable) throws MatlabInvocationException;
    
    /**
     * Implementers can be notified when a proxy becomes disconnected from MATLAB.
     * 
     * @see MatlabProxy#addDisconnectionListener(matlabcontrol.MatlabProxy.DisconnectionListener)
     * @since 4.0.0
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
         * Returns {@code true} if {@code other} is equal to this identifier, {@code false} otherwise.
         * 
         * @param other
         * @return 
         */
        @Override
        public boolean equals(Object other);
    }
}