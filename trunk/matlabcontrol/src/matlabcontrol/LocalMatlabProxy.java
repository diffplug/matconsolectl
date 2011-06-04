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
 * Allows for calling MATLAB from <b>inside</b> of MATLAB.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
final class LocalMatlabProxy implements MatlabProxy<Object>
{
    /**
     * The underlying wrapper to JMI.
     */
    private final JMIWrapper _wrapper;
    
    /**
     * Unique identifier for this proxy.
     */
    private final String _id;
    
    /**
     * The factory that created this proxy.
     */
    private final LocalMatlabProxyFactory _factory;

    LocalMatlabProxy(JMIWrapper wrapper, String id, LocalMatlabProxyFactory factory)
    {
        _wrapper = wrapper;
        _id = id;
        _factory = factory;
    }

    @Override
    public void exit() throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            _wrapper.exit();
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }
    
    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            _wrapper.eval(command);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }
    
    @Override
    public Object returningEval(String command, int returnCount) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return _wrapper.returningEval(command, returnCount);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }
    
    @Override
    public void feval(String functionName, Object[] args) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            _wrapper.feval(functionName, args);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }
    
    @Override
    public Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return _wrapper.returningFeval(functionName, args);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }

    @Override
    public Object returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return _wrapper.returningFeval(functionName, args, returnCount);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }

    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            _wrapper.setVariable(variableName, value);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }
    
    @Override
    public Object getVariable(String variableName) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return _wrapper.getVariable(variableName);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }

    @Override
    public void setDiagnosticMode(boolean enable) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            _wrapper.setDiagnosticMode(enable);
        }
        else
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
    }

    @Override
    public String getIdentifier()
    {
        return _id;
    }
    
    @Override
    public boolean isConnected()
    {
        return !_factory.isShutdown();
    }
    
    @Override
    public String toString()
    {
        return "[LocalMatlabProxy identifier:" + _id + "]";
    }
}