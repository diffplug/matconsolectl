package matlabcontrol.extensions;

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
import java.util.concurrent.ThreadFactory;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabProxy;

/**
 * Wraps around a {@link MatlabProxy} making the method calls which interact with MATLAB operate with callbacks instead
 * of return values. For each method in {@code MatlabProxy} that interacts with MATLAB, the same method exists but has
 * one additional parameter that is either {@link MatlabCallback} or {@link MatlabDataCallback}. Method invocations do
 * not throw {@link MatlabInvocationException}s, but if the proxy throws a {@code MatlabInvocationException} it will be
 * provided to the callback.
 * <br><br>
 * All interactions with the proxy will be done in a single threaded manner. The underlying proxy methods will be
 * completed in the order their corresponding methods in this class were called. Because method invocations on the
 * proxy occur on a separate thread from the one calling the methods in this class, it can be used from within MATLAB on
 * the Event Dispatch Thread (EDT).
 * <br><br>
 * This class is unconditionally thread-safe. There are no guarantees about the relative ordering of method completion
 * when methods are invoked both on an instance of this class and on the proxy provided to it.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class CallbackMatlabProxy
{
    /**
     * Executor that manages the single daemon thread used to invoke methods on the proxy. 
     */
    private final ExecutorService _executor = Executors.newFixedThreadPool(1, new DaemonThreadFactory()); 
    
    private final MatlabProxy _proxy;
    
    /**
     * Constructs an instance of this class, delegating all method invocations to the {@code proxy}.
     * 
     * @param proxy 
     */
    public CallbackMatlabProxy(MatlabProxy proxy)
    {
        _proxy = proxy;
    }
        
    /**
     * Returns a brief description. The exact details of this representation are unspecified and are subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " proxy=" + _proxy + "]";
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     */
    public void isConnected(final MatlabDataCallback<Boolean> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                boolean connected = _proxy.isConnected();
                callback.invocationSucceeded(connected);
            }
        });
    }
    
    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     */
    public void disconnect(final MatlabDataCallback<Boolean> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                boolean succeeded = _proxy.disconnect();
                callback.invocationSucceeded(succeeded);
            }
        });
    }
    
    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     */
    public void exit(final MatlabCallback callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _proxy.exit();
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param command
     */
    public void eval(final MatlabCallback callback, final String command)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _proxy.eval(command);
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param command
     * @param nargout
     */
    public void returningEval(final MatlabDataCallback<Object[]> callback, final String command, final int nargout)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Object[] data = _proxy.returningEval(command, nargout);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param functionName
     * @param args
     */
    public void feval(final MatlabCallback callback, final String functionName, final Object... args)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _proxy.feval(functionName, args);
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param functionName
     * @param nargout
     * @param args
     */
    public void returningFeval(final MatlabDataCallback<Object[]> callback, final String functionName,
            final int nargout, final Object... args)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Object[] data = _proxy.returningFeval(functionName, nargout, args);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param variableName
     * @param value
     */
    public void setVariable( final MatlabCallback callback, final String variableName, final Object value)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _proxy.setVariable(variableName, value);
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the proxy's corresponding method has completed.
     * 
     * @param callback 
     * @param variableName
     */
    public void getVariable(final MatlabDataCallback<Object> callback, final String variableName)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Object data = _proxy.getVariable(variableName);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }
    
    /**
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
     * <br><br>
     * The name of this method has been retained for consistency with {@code MatlabProxy}, but note that while the
     * code in the {@code callable} will be invoked on the MATLAB thread and it will wait until completion so as to
     * return a result, this method - like all others in this class, will not wait for completion. Instead, the result
     * will be provided to the {@code callback}.
     * 
     * @param callable
     * @param callback 
     */
    public <U> void invokeAndWait(final MatlabThreadCallable<U> callable, final MatlabDataCallback<U> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    U data = _proxy.invokeAndWait(callable);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }
    
    /**
     * A callback that supplies the results of the invocation or the raised exception.
     * 
     * @param <V> 
     */
    public static interface MatlabDataCallback<V>
    {
        /**
         * Called when the method successfully completed.
         * 
         * @param data the data returned from MATLAB
         */
        public void invocationSucceeded(V data);
        
        /**
         * Called when the method failed.
         * 
         * @param e the exception thrown 
         */
        public void invocationFailed(MatlabInvocationException e);
    }
    
    /**
     * A callback that indicates either the invocation succeeding or an exception being raised.
     */
    public static interface MatlabCallback
    {
        /**
         * Called when the method successfully completed.
         */
        public void invocationSucceeded();
        
        /**
         * Called when the method failed.
         * 
         * @param e the exception thrown 
         */
        public void invocationFailed(MatlabInvocationException e);
    }
    
    /**
     * Creates daemon threads that will allow the JVM to terminate.
     */
    private static class DaemonThreadFactory implements ThreadFactory
    {
        private final ThreadFactory _delegateFactory = Executors.defaultThreadFactory();
        
        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = _delegateFactory.newThread(r);
            thread.setName("MatlabCallbackInteractor Thread");
            thread.setDaemon(true);
            
            return thread;
        }
    }
}