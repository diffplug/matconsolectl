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

import java.rmi.RemoteException;
import java.rmi.UnmarshalException;

/**
 * Allows for calling MATLAB from <strong>outside</strong> of MATLAB.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
final class RemoteMatlabProxy extends MatlabProxy
{
    /**
     * The underlying proxy which is a remote object connected over RMI.
     */ 
    private final JMIWrapperRemote _internalProxy;
    
    /**
     * Unique identifier for this proxy.
     */
    private final String _id;
    
    /**
     * Whether the session of MATLAB this proxy is connected to is an existing session.
     */
    private final boolean _existingSession;
    
    /**
     * The proxy is never to be created outside of this package, it is to be constructed after a
     * {@link JMIWrapperRemote} has been received via RMI.
     * 
     * @param internalProxy
     */
    RemoteMatlabProxy(JMIWrapperRemote internalProxy, String id, boolean existingSession)
    {
        _internalProxy = internalProxy;
        _id = id;
        _existingSession = existingSession;
    }
    
    @Override
    public String getIdentifier()
    {
        return _id;
    }
    
    @Override
    public boolean isConnected()
    {
        boolean connected;
        try
        {
            _internalProxy.checkConnection();    
            connected = true;
        }
        catch(Exception e)
        {
            connected = false;
        }
        
        return connected;
    }
    
    @Override
    public boolean isExistingSession()
    {
        return _existingSession;
    }
    
    private static interface RemoteVoidInvocation
    {
        public void invoke() throws RemoteException, MatlabInvocationException;
    }
    
    private static interface RemoteReturningInvocation<T>
    {
        public T invoke() throws RemoteException, MatlabInvocationException;
    }

    private void invoke(RemoteVoidInvocation invocation) throws MatlabInvocationException
    {
        try
        {
            invocation.invoke();
        }
        catch (RemoteException e)
        {
            if(this.isConnected())
            {
                throw new MatlabInvocationException(MatlabInvocationException.UNKNOWN_REMOTE_REASON_MSG, e);
            }
            else
            {
                throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG, e);
            }
        }
    }

    private <T> T invoke(RemoteReturningInvocation<T> invocation) throws MatlabInvocationException
    {
        try
        {
            return invocation.invoke();
        }
        catch(UnmarshalException e)
        {
            throw new MatlabInvocationException(MatlabInvocationException.UNMARSHALLING_MSG, e);
        }
        catch (RemoteException e)
        {
            if(this.isConnected())
            {
                throw new MatlabInvocationException(MatlabInvocationException.UNKNOWN_REMOTE_REASON_MSG, e);
            }
            else
            {
                throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG, e);
            }
        }
    }
    
    @Override
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _internalProxy.setVariable(variableName, value);
            }
        });
    }
    
    @Override
    public Object getVariable(final String variableName) throws MatlabInvocationException
    {
        return this.invoke(new RemoteReturningInvocation<Object>()
        {
            @Override
            public Object invoke() throws RemoteException, MatlabInvocationException
            {
                return _internalProxy.getVariable(variableName);
            }
        });
    }
    
    @Override
    public void exit() throws MatlabInvocationException
    {
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _internalProxy.exit();
            }
        });
    }
    
    @Override
    public void eval(final String command) throws MatlabInvocationException
    {
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _internalProxy.eval(command);
            }
        });
    }

    @Override
    public Object returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return this.invoke(new RemoteReturningInvocation<Object>()
        {
            @Override
            public Object invoke() throws RemoteException, MatlabInvocationException
            {
                return _internalProxy.returningEval(command, returnCount);
            }
        });
    }

    @Override
    public void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _internalProxy.feval(functionName, args);
            }
        });
    }
    
    @Override
    public Object returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        return this.invoke(new RemoteReturningInvocation<Object>()
        {
            @Override
            public Object invoke() throws RemoteException, MatlabInvocationException
            {
                return _internalProxy.returningFeval(functionName, args);
            }
        });
    }
    
    @Override
    public Object returningFeval(final String functionName, final Object[] args, final int returnCount) throws MatlabInvocationException
    {
        return this.invoke(new RemoteReturningInvocation<Object>()
        {
            @Override
            public Object invoke() throws RemoteException, MatlabInvocationException
            {
                return _internalProxy.returningFeval(functionName, args, returnCount);
            }
        });
    }

    @Override
    public void setDiagnosticMode(final boolean enable) throws MatlabInvocationException
    {        
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _internalProxy.setDiagnosticMode(enable);
            }
        });
    }
    
    @Override
    public String toString()
    {
        return "[RemoteMatlabProxy identifier:" + _id + "]";
    }
}