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

import matlabcontrol.MatlabInteractor;
import matlabcontrol.MatlabProxy;

/**
 * Wraps around a proxy to conveniently handle casts of the data returned from MATLAB.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class ReturnDataMatlabInteractor implements MatlabInteractor<ReturnData>
{
    private final MatlabProxy _proxy;
    
    /**
     * Constructs this interactor, delegating all method calls to the {@code proxy}.
     * 
     * @param proxy 
     */
    public ReturnDataMatlabInteractor(MatlabProxy proxy)
    {   
        _proxy = proxy;
    }

    /**
     * Delegates to the proxy.
     * 
     * @param command
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public void eval(String command)
    {
        _proxy.eval(command);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param command
     * @param returnCount
     * @return
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public ReturnData returningEval(String command, int returnCount)
    {
        return new ReturnData(_proxy.returningEval(command, returnCount));
    }

    /**
     * Delegates to the proxy.
     * 
     * @param functionName
     * @param args
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public void feval(String functionName, Object[] args)
    {
        _proxy.feval(functionName, args);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param functionName
     * @param args
     * @return
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public ReturnData returningFeval(String functionName, Object[] args)
    {
        return new ReturnData(_proxy.returningFeval(functionName, args));
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param functionName
     * @param args
     * @param returnCount
     * @return
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public ReturnData returningFeval(String functionName, Object[] args, int returnCount)
    {
        return new ReturnData(_proxy.returningFeval(functionName, args, returnCount));
    }

    /**
     * Delegates to the proxy.
     * 
     * @param variableName
     * @param value
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public void setVariable(String variableName, Object value)
    {
        _proxy.setVariable(variableName, value);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param variableName
     * @return
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public ReturnData getVariable(String variableName)
    {
        return new ReturnData(_proxy.getVariable(variableName));
    }

    /**
     * Delegates to the proxy. The return of this method cannot be wrapped in a {@link ReturnData} due to type
     * mismatch in method signature, so it returns data of type {@code T} instead.
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    @Override
    public <T> T invokeAndWait(MatlabCallable<T> callable)
    {
        return _proxy.invokeAndWait(callable);
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
        return "[" + this.getClass().getName() + " delegate=" + _proxy + "]";
    }
}