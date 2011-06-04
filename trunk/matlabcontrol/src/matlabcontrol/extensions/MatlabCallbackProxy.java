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
 *
 */
public class MatlabCallbackProxy<E>
{
    private final ExecutorService _executor = Executors.newSingleThreadExecutor();
    private MatlabProxy<E> _proxy;
    
    public static <T> MatlabCallbackProxy<T> getProxy(MatlabProxy<T> proxy)
    {
        return new MatlabCallbackProxy<T>(proxy);
    }
    
    public MatlabCallbackProxy(MatlabProxy<E> proxy)
    {
        _proxy = proxy;
    }

    public boolean isConnected()
    {
        return _proxy.isConnected();
    }

    public String getIdentifier()
    {
        return _proxy.getIdentifier();
    }

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

    public void eval(final String command, final MatlabCallback callback)
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

    public void returningEval(final String command, final int returnCount, final MatlabDataCallback<E> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _proxy.returningEval(command, returnCount);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    public void feval(final String functionName, final Object[] args, final MatlabCallback callback)
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

    public void returningFeval(final String functionName, final Object[] args, final MatlabDataCallback<E> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _proxy.returningFeval(functionName, args);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    public void returningFeval(final String functionName, final Object[] args, final int returnCount, final MatlabDataCallback<E> callback)
    {
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _proxy.returningFeval(functionName, args, returnCount);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    public void setVariable(final String variableName, final Object value, final MatlabCallback callback)
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

    public void getVariable(final String variableName, final MatlabDataCallback<E> callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    E data = _proxy.getVariable(variableName);
                    callback.invocationSucceeded(data);
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }

    public void setDiagnosticMode(final boolean enable, final MatlabCallback callback)
    {        
        _executor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    _proxy.setDiagnosticMode(enable);
                    callback.invocationSucceeded();
                }
                catch(MatlabInvocationException e)
                {
                    callback.invocationFailed(e);
                }
            }       
        });
    }
    
    public static interface MatlabDataCallback<E>
    {
        public void invocationSucceeded(E data);
        public void invocationFailed(MatlabInvocationException e);
    }
    
    public static interface MatlabCallback
    {
        public void invocationSucceeded();
        public void invocationFailed(MatlabInvocationException e);
    }
}