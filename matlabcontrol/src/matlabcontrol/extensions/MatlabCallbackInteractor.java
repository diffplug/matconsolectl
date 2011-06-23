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
import matlabcontrol.MatlabInteractor;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;

/**
 * Wraps around an interactor making the method calls operate with callbacks instead of return values. Due to this
 * difference this class does not implement {@link MatlabInteractor}, but it closely matches the methods. For each
 * method in {@code MatlabInteractor} the same method exists but has one additional parameter that is either
 * {@link MatlabCallback} or {@link MatlabDataCallback}. Method invocations do not throw exceptions, but if the
 * interactor throws an exception it will be provided to the callback.
 * <br><br>
 * This class is thread-safe even if the interactor provided to it is not thread-safe. All interactions with the
 * interactor will be done in a single threaded manner. Because method invocations on the delegate interactor occur on
 * a separate thread from the one calling the methods in this class, it can be used from within MATLAB on the Event
 * Dispatch Thread (EDT).
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabCallbackInteractor<E>
{
    /**
     * Executor that manages the single daemon thread used to invoke methods on the interactor. 
     */
    private final ExecutorService _executor = Executors.newFixedThreadPool(1, new DaemonThreadFactory()); 
    
    /**
     * The interactor delegated to.
     */
    private final MatlabInteractor<E> _delegateInteractor;
    
    /**
     * Constructs this interactor which will delegate to the provided {@code interactor}. The type returned by the
     * delegate {@code interactor} is the same type that will be returned in the callbacks. A
     * {@link matlabcontrol.MatlabProxy} is a {@code MatlabInteractor} that returns {@code Object}s. To use with a
     * {@code MatlabProxy}:
     * <br><br>
     * {@code
     * MatlabCallbackInteractor<Object> callbackInteractor = new MatlabCallbackInteractor<Object>(proxy);
     * }
     * 
     * @param interactor 
     */
    public MatlabCallbackInteractor(MatlabInteractor<E> interactor)
    {
        _delegateInteractor = interactor;
    }
        
    /**
     * Returns a brief description of this interactor. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " delegate=" + _delegateInteractor + "]";
    }

    /**
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param command
     * @param callback 
     */
    public void eval(final String command, final MatlabCallback callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _delegateInteractor.eval(command);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param command
     * @param returnCount
     * @param callback 
     */
    public void returningEval(final String command, final int returnCount, final MatlabDataCallback<E> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _delegateInteractor.returningEval(command, returnCount);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param functionName
     * @param args
     * @param callback 
     */
    public void feval(final String functionName, final Object[] args, final MatlabCallback callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _delegateInteractor.feval(functionName, args);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param functionName
     * @param args
     * @param callback 
     */
    public void returningFeval(final String functionName, final Object[] args, final MatlabDataCallback<E> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _delegateInteractor.returningFeval(functionName, args);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param functionName
     * @param args
     * @param returnCount
     * @param callback 
     */
    public void returningFeval(final String functionName, final Object[] args, final int returnCount,
            final MatlabDataCallback<E> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _delegateInteractor.returningFeval(functionName, args, returnCount);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param variableName
     * @param value
     * @param callback 
     */
    public void setVariable(final String variableName, final Object value, final MatlabCallback callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _delegateInteractor.setVariable(variableName, value);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * 
     * @param variableName
     * @param callback 
     */
    public void getVariable(final String variableName, final MatlabDataCallback<E> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _delegateInteractor.getVariable(variableName);
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
     * Delegates to the interactor, calling the {@code callback} when the method has been executed.
     * The name of this method has been retained for consistency with {@code MatlabInteractor}, but not that while the
     * code in the callable will be invoked on the MATLAB thread and it will wait until completion so as to return a
     * result, this method - like all others in this class, will not wait for completion. Instead, the result will be
     * provided to the {@code callback}.
     * 
     * @param callable
     * @param callback 
     */
    public <T> void invokeAndWait(final MatlabThreadCallable<T> callable, final MatlabDataCallback<T> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    T data = _delegateInteractor.invokeAndWait(callable);
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
     * @param <E> 
     */
    public static interface MatlabDataCallback<E>
    {
        /**
         * Called when the method successfully completed.
         * 
         * @param data the data returned from MATLAB
         */
        public void invocationSucceeded(E data);
        
        /**
         * Called when the method failed.
         * 
         * @param e the exception raised 
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
         * @param e the exception raised 
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
            thread.setDaemon(true);
            
            return thread;
        }
    }
}