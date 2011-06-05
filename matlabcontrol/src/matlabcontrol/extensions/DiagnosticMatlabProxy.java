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

import java.io.PrintStream;
import java.lang.reflect.Array;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 * Wraps around a proxy to provide a log of interactions. The data is not altered.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class DiagnosticMatlabProxy<E> implements MatlabProxy<E>
{
    private final MatlabProxy<E> _delegateProxy;
    private final PrintStream _printStream;
    
    /**
     * Constructs the proxy, printing the interaction to provided {@code printStream}.
     * 
     * @param proxy
     * @param printStream 
     */
    public DiagnosticMatlabProxy(MatlabProxy<E> proxy, PrintStream printStream)
    {
        _delegateProxy = proxy;
        _printStream = printStream;
    }
    
    /**
     * Constructs the proxy, using {@code System.out} as the {@code PrintStream}.
     * 
     * @param proxy 
     */
    public DiagnosticMatlabProxy(MatlabProxy<E> proxy)
    {
        this(proxy, System.out);
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @return 
     */
    @Override
    public boolean isConnected()
    {
        _printStream.println("--------------------------------------------------------------------------------");
        _printStream.println("Invoking: isConnected()");
        
        boolean connected = _delegateProxy.isConnected();
        
        _printStream.println("Successfully invoked, returned:");
        _printStream.println(connected);
        _printStream.println("--------------------------------------------------------------------------------");
        
        return connected;
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @return 
     */
    @Override
    public String getIdentifier()
    {
        _printStream.println("--------------------------------------------------------------------------------");
        _printStream.println("Invoking: getIdentifier()");
        
        String id = _delegateProxy.getIdentifier();
        
        _printStream.println("Successfully invoked, returned:");
        _printStream.println(id);
        _printStream.println("--------------------------------------------------------------------------------");
        
        return id;
    }
    
    private static interface VoidInvocation
    {
        public void invoke() throws MatlabInvocationException;
        public String getName();
    }
    
    private static interface ReturningInvocation<T>
    {
        public T invoke() throws MatlabInvocationException;
        public String getName();
    }

    private void invoke(VoidInvocation invocation) throws MatlabInvocationException
    {
        _printStream.println("--------------------------------------------------------------------------------");
        _printStream.println("Invoking: " + invocation.getName());
        
        try
        {
            invocation.invoke();
            
            _printStream.println("Successfully invoked");
            _printStream.println("--------------------------------------------------------------------------------");
        }
        catch(MatlabInvocationException e)
        {            
            _printStream.println("Exception encountered:");
            e.printStackTrace(_printStream);
            _printStream.println("--------------------------------------------------------------------------------");
            
            throw e;
        }
    }

    private <T> T invoke(ReturningInvocation<T> invocation) throws MatlabInvocationException
    {
        T data;
        
        _printStream.println("--------------------------------------------------------------------------------");
        _printStream.println("Invoking: " + invocation.getName());
        try
        {
            data = invocation.invoke();
            
            _printStream.println("Successfully invoked, returned:");
            formatResult(data, 0, _printStream);
            _printStream.println("--------------------------------------------------------------------------------");
        }
        catch(MatlabInvocationException e)
        {
            _printStream.println("Exception encountered:");
            e.printStackTrace(_printStream);
            _printStream.println("--------------------------------------------------------------------------------");
            
            throw e;
        }
        
        return data;
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @throws MatlabInvocationException 
     */
    @Override
    public void exit() throws MatlabInvocationException
    {
        this.invoke(new VoidInvocation()
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateProxy.exit();
            }

            @Override
            public String getName()
            {
                return "exit()";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param command
     * @throws MatlabInvocationException 
     */
    @Override
    public void eval(final String command) throws MatlabInvocationException
    {           
        this.invoke(new VoidInvocation()
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateProxy.eval(command);
            }

            @Override
            public String getName()
            {
                return "eval(String)";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param command
     * @param returnCount
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>()
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateProxy.returningEval(command, returnCount);
            }

            @Override
            public String getName()
            {
                return "returningEval(String, int)";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param functionName
     * @param args
     * @throws MatlabInvocationException 
     */
    @Override
    public void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        this.invoke(new VoidInvocation()
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateProxy.feval(functionName, args);
            }

            @Override
            public String getName()
            {
                return "feval(String, Object[])";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param functionName
     * @param args
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>()
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateProxy.returningFeval(functionName, args);
            }

            @Override
            public String getName()
            {
                return "returningFeval(String, Object[])";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param functionName
     * @param args
     * @param returnCount
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E returningFeval(final String functionName, final Object[] args, final int returnCount) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>()
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateProxy.returningFeval(functionName, args, returnCount);
            }

            @Override
            public String getName()
            {
                return "returningFeval(String, Object[], int)";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException 
     */
    @Override
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {   
        this.invoke(new VoidInvocation()
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateProxy.setVariable(variableName, value);
            }

            @Override
            public String getName()
            {
                return "setVariable(String, int)";
            }
        });
    }

    @Override
    public E getVariable(final String variableName) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>()
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateProxy.getVariable(variableName);
            }

            @Override
            public String getName()
            {
                return "getVariable(String)";
            }
        });
    }

    /**
     * Delegates to the proxy; prints the interaction to the {@code PrintStream}.
     * 
     * @param enable
     * @throws MatlabInvocationException 
     */
    @Override
    public void setDiagnosticMode(final boolean enable) throws MatlabInvocationException
    {
        this.invoke(new VoidInvocation()
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateProxy.setDiagnosticMode(enable);
            }

            @Override
            public String getName()
            {
                return "setDiagnosticMode(boolean)";
            }
        });
    }
    
    @Override
    public String toString()
    {
        return "[DiagnosticMatlabProxy delegate:" + _delegateProxy + "]";
    }
    
    /**
     * Takes in the result from MATLAB and turns it into an easily readable format.
     * 
     * @param result
     * @param level, pass in 0 to initialize, used recursively
     * @return description
     */
    private static void formatResult(Object result, int level, PrintStream stream)
    {   
        //Tab offset for levels
        String tab = "";
        for(int i = 0; i < level + 1; i++)
        {
            tab += "  ";
        }
        
        //If the result is null
        if(result == null)
        {
            stream.print("null encountered\n");
        }
        //If the result is an array
        else if(result.getClass().isArray())
        {
            Class<?> componentClass = result.getClass().getComponentType();
            
            //Primitive array
            if(componentClass.isPrimitive())
            {
                String componentName = componentClass.toString();
                int length = Array.getLength(result);
                
                stream.print(componentName);
                stream.print(" array, length = ");
                stream.print(length);
                stream.print("\n");
                
                for(int i = 0; i < length; i++)
                {   
                    stream.print(tab);
                    stream.print("index ");
                    stream.print(i);
                    stream.print(", ");
                    stream.print(componentName);
                    stream.print(": ");
                    stream.print(Array.get(result, i));
                    stream.print("\n");
                }
            }
            //Object array
            else
            {
                Object[] array = (Object[]) result;
                
                stream.print("Object array, length = ");
                stream.print(array.length);
                stream.print("\n");
                
                for(int i = 0; i < array.length; i++)
                {   
                    stream.print(tab);
                    stream.print("index ");
                    stream.print(i);
                    stream.print(", ");
                    formatResult(array[i], level + 1, stream);
                }
            }
        }
        //If an Object and not an array
        else
        {   
            stream.print(result.getClass().getCanonicalName());
            stream.print(": ");
            stream.print(result);
            stream.print("\n");
        }
    }
}