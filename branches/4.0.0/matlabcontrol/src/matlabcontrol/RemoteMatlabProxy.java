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

import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Allows for calling MATLAB from <strong>outside</strong> of MATLAB.
 * 
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class RemoteMatlabProxy extends MatlabProxy
{
    /**
     * The remote JMI wrapper which is a remote object connected over RMI.
     */ 
    private final JMIWrapperRemote _jmiWrapper;
    
    /**
     * The receiver for the proxy. While the receiver is bound to the RMI registry and a reference is maintained
     * (the RMI registry uses weak references), the connection to MATLAB's JVM will remain active. The JMI wrapper,
     * while a remote object, is not bound to the registry, and will not keep the RMI thread running.
     */
    private final RequestReceiver _receiver;
    
    /**
     * A timer that periodically checks if still connected.
     */
    private final Timer _connectionTimer = new Timer();
    
    /**
     * Whether the proxy is connected. If the value is {@code false} the proxy is definitely disconnected. If the value
     * is {@code true} then the proxy <i>may</i> be disconnected. The accuracy of this will be checked then the next
     * time {@link #isConnected()} is called and this value will be updated if necessary.
     */
    private volatile boolean _isConnected = true;
    
    /**
     * The duration (in milliseconds) between checks to determine if still connected.
     */
    private static final int CONNECTION_CHECK_PERIOD = 1000;
    
    /**
     * The proxy is never to be created outside of this package, it is to be constructed after a
     * {@link JMIWrapperRemote} has been received via RMI.
     * 
     * @param internalProxy
     * @param receiver
     * @param id
     * @param existingSession
     */
    RemoteMatlabProxy(JMIWrapperRemote internalProxy, RequestReceiver receiver, Identifier id, boolean existingSession)
    {
        super(id, existingSession);
        
        _jmiWrapper = internalProxy;
        _receiver = receiver;
    }
    
    /**
     * Initializes aspects of the proxy that cannot be done safely in the constructor without leaking a reference to
     * {@code this}.
     */
    void init()
    {
        _connectionTimer.schedule(new CheckConnectionTask(), CONNECTION_CHECK_PERIOD, CONNECTION_CHECK_PERIOD);
    }
    
    private class CheckConnectionTask extends TimerTask
    {
        @Override
        public void run()
        {
            if(!RemoteMatlabProxy.this.isConnected())
            {
                //If not connected, perform disconnection so RMI thread can terminate
                RemoteMatlabProxy.this.disconnect();

                //Notify listeners
                notifyDisconnectionListeners();

                //Cancel task, which will terminate the timer's thread
                this.cancel();
            }
        }
    }
        
    @Override
    public boolean isRunningInsideMatlab()
    {
        return false;
    }
    
    @Override
    public boolean isConnected()
    {
        //If believed to be connected, verify this is up to date information
        if(_isConnected)
        {
            boolean connected;
            //Call a remote method, if it throws a RemoteException then it is no longer connected
            try
            {
                _jmiWrapper.checkConnection();    
                connected = true;
            }
            catch(RemoteException e)
            {
                connected = false;
            }
            
            _isConnected = connected;
        }
        
        return _isConnected;
    }
    
    @Override
    public boolean disconnect()
    {
        //Unexport the receiver so that the RMI threads can shut down
        try
        {
            //If succesfully exported, then definitely not connected
            //If the export failed, we still might be connected, isConnected() will check
            _isConnected = !UnicastRemoteObject.unexportObject(_receiver, true);
        }
        //If it is not exported, that's ok because we were trying to unexport it
        catch(NoSuchObjectException e) { }
        
        return this.isConnected();
    }
    
    // Methods which interact with MATLAB (and helper methods and interfaces)
    
    private static interface RemoteInvocation<T>
    {
        public T invoke() throws RemoteException, MatlabInvocationException;
    }

    private <T> T invoke(RemoteInvocation<T> invocation) throws MatlabInvocationException
    {
        if(!_isConnected)
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
        else
        {
            try
            {
                return invocation.invoke();
            }
            catch(UnmarshalException e)
            {
                throw MatlabInvocationException.Reason.UNMARSHAL.asException(e);
            }
            catch(MarshalException e)
            {
                throw MatlabInvocationException.Reason.MARSHAL.asException(e);
            }
            catch(RemoteException e)
            {
                if(this.isConnected())
                {
                    throw MatlabInvocationException.Reason.UNKNOWN.asException(e);
                }
                else
                {
                    throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException(e);
                }
            }
        }
    }
    
    @Override
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {
        this.invoke(new RemoteInvocation<Void>()
        {
            @Override
            public Void invoke() throws RemoteException, MatlabInvocationException
            {
                _jmiWrapper.setVariable(variableName, value);
                
                return null;
            }
        });
    }
    
    @Override
    public Object getVariable(final String variableName) throws MatlabInvocationException
    {
        return this.invoke(new RemoteInvocation<Object>()
        {
            @Override
            public Object invoke() throws RemoteException, MatlabInvocationException
            {
                return _jmiWrapper.getVariable(variableName);
            }
        });
    }
    
    @Override
    public void exit() throws MatlabInvocationException
    {
        this.invoke(new RemoteInvocation<Void>()
        {
            @Override
            public Void invoke() throws RemoteException, MatlabInvocationException
            {
                _jmiWrapper.exit();
                
                return null;
            }
        });
    }
    
    @Override
    public void eval(final String command) throws MatlabInvocationException
    {
        this.invoke(new RemoteInvocation<Void>()
        {
            @Override
            public Void invoke() throws RemoteException, MatlabInvocationException
            {
                _jmiWrapper.eval(command);
                
                return null;
            }
        });
    }

    @Override
    public Object[] returningEval(final String command, final int nargout) throws MatlabInvocationException
    {
        return this.invoke(new RemoteInvocation<Object[]>()
        {
            @Override
            public Object[] invoke() throws RemoteException, MatlabInvocationException
            {
                return _jmiWrapper.returningEval(command, nargout);
            }
        });
    }

    @Override
    public void feval(final String functionName, final Object... args) throws MatlabInvocationException
    {
        this.invoke(new RemoteInvocation<Void>()
        {
            @Override
            public Void invoke() throws RemoteException, MatlabInvocationException
            {
                _jmiWrapper.feval(functionName, args);
                
                return null;
            }
        });
    }
    
    @Override
    public Object[] returningFeval(final String functionName, final int nargout, final Object... args)
            throws MatlabInvocationException
    {
        return this.invoke(new RemoteInvocation<Object[]>()
        {
            @Override
            public Object[] invoke() throws RemoteException, MatlabInvocationException
            {
                return _jmiWrapper.returningFeval(functionName, nargout, args);
            }
        });
    }
    
    @Override
    public <T> T invokeAndWait(final MatlabThreadCallable<T> callable) throws MatlabInvocationException
    {
        return this.invoke(new RemoteInvocation<T>()
        {
            @Override
            public T invoke() throws RemoteException, MatlabInvocationException
            {
                return _jmiWrapper.invokeAndWait(callable);
            }
        });
    }
}