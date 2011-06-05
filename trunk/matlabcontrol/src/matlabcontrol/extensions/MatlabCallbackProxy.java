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

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 * Wraps around a proxy making the method calls operate with callbacks instead of return values. Due to this difference
 * this class does not implement {@link MatlabProxy}, but it closely matches the methods. For each method that calls
 * MATLAB in {@code MatlabProxy} the same method exists but has one additional parameter that is either
 * {@link MatlabCallbackProxy.MatlabCallback} or {@link MatlabCallbackProxy.MatlabDataCallback}. Because the actual
 * proxy invocation occurs on a separate thread from the one calling the methods in this class, it can be used from
 * within MATLAB on the Event Dispatch Thread (EDT).
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabCallbackProxy<E>
{
    private final ExecutorService _executor = Executors.newSingleThreadExecutor();
    private MatlabProxy<E> _delegateProxy;
    
    public MatlabCallbackProxy(MatlabProxy<E> proxy)
    {
        _delegateProxy = proxy;
    }

    /**
     * Delegates to the proxy without a callback.
     * 
     * @return 
     */
    public boolean isConnected()
    {
        return _delegateProxy.isConnected();
    }

    /**
     * Delegates to the proxy without a callback.
     * 
     * @return 
     */
    public String getIdentifier()
    {
        return _delegateProxy.getIdentifier();
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
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
                    _delegateProxy.exit();
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
                finally
                {
                    _executor.shutdown();
                }
            }       
        });
    }

    /**
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
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
                    _delegateProxy.eval(command);
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
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
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
                    E data = _delegateProxy.returningEval(command, returnCount);
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
                    _delegateProxy.feval(functionName, args);
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
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
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
                    E data = _delegateProxy.returningFeval(functionName, args);
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
     * 
     * @param functionName
     * @param args
     * @param returnCount
     * @param callback 
     */
    public void returningFeval(final String functionName, final Object[] args, final int returnCount, final MatlabDataCallback<E> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _delegateProxy.returningFeval(functionName, args, returnCount);
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
                    _delegateProxy.setVariable(variableName, value);
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
     * Delegates to the proxy, calling the {@code callback} when the method has been executed.
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
                    E data = _delegateProxy.getVariable(variableName);
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
     * 
     * @param enable
     * @param callback 
     */
    public void setDiagnosticMode(final boolean enable, final MatlabCallback callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _delegateProxy.setDiagnosticMode(enable);
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }
    
    @Override
    public String toString()
    {
        return "[MatlabCallbackProxy delegate:" + _delegateProxy + "]";
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
}