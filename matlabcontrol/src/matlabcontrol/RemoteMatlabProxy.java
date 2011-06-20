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

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final JMIWrapperRemoteReceiver _receiver;
    
    /**
     * A timer that periodically checks if still connected.
     */
    private final Timer _connectionTimer;
    
    /**
     * Whether the proxy has become disconnected. This is either because the connection to MATLAB has been actually
     * been lost or a request came in to make that occur.
     */
    //private volatile boolean _isDisconnected = false;
    
    /**
     * Whether the proxy is connected. This variable could be {@code true} but no actual connection exists. It will be
     * updated the next time {@link #isConnected()} is called.
     */
    private volatile boolean _isConnected = true;
    
    /**
     * The duration (in milliseconds) between checks to determine if still connected.
     */
    private static final int CONNECTION_CHECK_PERIOD = 1000;
    
    /**
     * Listeners for disconnection.
     */
    private final CopyOnWriteArrayList<DisconnectionListener> _listeners;
    
    /**
     * The proxy is never to be created outside of this package, it is to be constructed after a
     * {@link JMIWrapperRemote} has been received via RMI.
     * 
     * @param internalProxy
     * @param receiver
     * @param id
     * @param existingSession
     */
    RemoteMatlabProxy(JMIWrapperRemote internalProxy, JMIWrapperRemoteReceiver receiver, Identifier id,
            boolean existingSession)
    {
        super(id, existingSession);
        
        _jmiWrapper = internalProxy;
        _receiver = receiver;
        
        _connectionTimer = createTimer();
        _listeners = new CopyOnWriteArrayList<DisconnectionListener>();
    }
    
    @Override
    public boolean isConnected()
    {
        boolean connected;
        
        //If connected, very this is up to date information
        if(_isConnected)
        {
            try
            {
                _jmiWrapper.checkConnection();    
                connected = true;
            }
            catch(Exception e)
            {
                connected = false;
            }
            
            _isConnected = connected;
        }
        //If no longer connected, there is no possibility of becoming reconnected
        else
        {
            connected = false;
        }
        
        return connected;
    }

    @Override
    public void addDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.add(listener);
    }

    @Override
    public void removeDisconnectionListener(DisconnectionListener listener)
    {
        _listeners.remove(listener);
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
        if(!_isConnected)
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
        else
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
    }

    private <T> T invoke(RemoteReturningInvocation<T> invocation) throws MatlabInvocationException
    {
        if(!_isConnected)
        {
            throw new MatlabInvocationException(MatlabInvocationException.PROXY_NOT_CONNECTED_MSG);
        }
        else
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
    }
    
    @Override
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {
        this.invoke(new RemoteVoidInvocation()
        {
            @Override
            public void invoke() throws RemoteException, MatlabInvocationException
            {
                _jmiWrapper.setVariable(variableName, value);
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
                return _jmiWrapper.getVariable(variableName);
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
                _jmiWrapper.exit();
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
                _jmiWrapper.eval(command);
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
                return _jmiWrapper.returningEval(command, returnCount);
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
                _jmiWrapper.feval(functionName, args);
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
                return _jmiWrapper.returningFeval(functionName, args);
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
                return _jmiWrapper.returningFeval(functionName, args, returnCount);
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
                _jmiWrapper.setDiagnosticMode(enable);
            }
        });
    }
    
    @Override
    public String storeObject(final Object obj, final boolean keepPermanently) throws MatlabInvocationException
    {
        return this.invoke(new RemoteReturningInvocation<String>()
        {
            @Override
            public String invoke() throws RemoteException, MatlabInvocationException
            {
                return _jmiWrapper.storeObject(obj, keepPermanently);
            }
        });
    }
    
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " identifier=" + getIdentifier() + "]";
    }
    
    @Override
    public boolean disconnect()
    {
        //Unexport the receiver so that the RMI threads can shut down
        boolean success;
        try
        {
            success = UnicastRemoteObject.unexportObject(_receiver, true);
            _isConnected = !success;
        }
        //If it is not exported, that's ok because we were trying to unexport it
        catch(NoSuchObjectException e)
        {
            success = true;
        }
        
        return success;
    }
    
    private Timer createTimer()
    {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {   
                if(!RemoteMatlabProxy.this.isConnected())
                {
                    //If not connected, perform disconnection so RMI thread can terminate
                    if(!_isConnected)
                    {
                        RemoteMatlabProxy.this.disconnect();
                    }
                    
                    //Notify listeners
                    for(DisconnectionListener listener : _listeners)
                    {
                        listener.proxyDisconnected(RemoteMatlabProxy.this);
                    }
                    
                    //Shutdown timer
                    timer.cancel();
                }
            }
        }, CONNECTION_CHECK_PERIOD, CONNECTION_CHECK_PERIOD);
        
        return timer;
    }
}