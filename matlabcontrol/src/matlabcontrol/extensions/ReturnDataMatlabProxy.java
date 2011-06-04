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

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 *
 */
public class ReturnDataMatlabProxy<E extends ReturnData> implements MatlabProxy<E>
{
    private final ReturnDataProvider<E> _provider;
    private final MatlabProxy _proxy;
    
    public static ReturnDataMatlabProxy<DefaultReturnData> getProxy(MatlabProxy<Object> proxy)
    {
        ReturnDataProvider<DefaultReturnData> provider = new ReturnDataProvider<DefaultReturnData>()
        {
            @Override
            public DefaultReturnData getMatlabReturnData(Object data)
            {
                return new DefaultReturnData(data);
            }       
        };
        
        return new ReturnDataMatlabProxy(proxy, provider);
    }
    
    public static <T extends ReturnData> ReturnDataMatlabProxy<T> getProxy(MatlabProxy<Object> proxy, ReturnDataProvider<T> provider)
    {
        return new ReturnDataMatlabProxy(proxy, provider);
    }
    
    private ReturnDataMatlabProxy(MatlabProxy proxy, ReturnDataProvider<E> provider)
    {
        _provider = provider;
        _proxy = proxy;
    }

    @Override
    public boolean isConnected()
    {
        return _proxy.isConnected();
    }

    @Override
    public String getIdentifier()
    {
        return _proxy.getIdentifier();
    }

    @Override
    public void exit() throws MatlabInvocationException
    {
        _proxy.exit();
    }

    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        _proxy.eval(command);
    }

    @Override
    public E returningEval(String command, int returnCount) throws MatlabInvocationException
    {
        return _provider.getMatlabReturnData(_proxy.returningEval(command, returnCount));
    }

    @Override
    public void feval(String functionName, Object[] args) throws MatlabInvocationException
    {
        _proxy.feval(functionName, args);
    }

    @Override
    public E returningFeval(String functionName, Object[] args) throws MatlabInvocationException
    {
        return _provider.getMatlabReturnData(_proxy.returningFeval(functionName, args));
    }

    @Override
    public E returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException
    {
        return _provider.getMatlabReturnData(_proxy.returningFeval(functionName, args, returnCount));
    }

    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        _proxy.setVariable(variableName, value);
    }

    @Override
    public E getVariable(String variableName) throws MatlabInvocationException
    {
        return _provider.getMatlabReturnData(_proxy.getVariable(variableName));
    }

    @Override
    public void setDiagnosticMode(boolean enable) throws MatlabInvocationException
    {
        _proxy.setDiagnosticMode(enable);
    }
}