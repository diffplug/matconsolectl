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

import java.lang.reflect.Array;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabInteractor;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;

/**
 * Wraps around an interactor to provide a log of interactions. The data is not altered. This interactor is very
 * useful for determining the Java types and structure of data returned from MATLAB.
 * <br><br>
 * Entering a method, exiting a method, and throwing an exception are logged. Method parameters and return values are
 * logged. The contents of a returned array can optionally be recursively explored. As is convention, all of these
 * interactions are logged at {@code Level.FINER}. If the logging system has not been otherwise configured, then the
 * {@code ConsoleHandler} which prints log messages to the console will not show these log messages as their level is
 * too low. To configure the {@code ConsoleHandler} to show these log messages, call {@link #showInConsoleHandler()}.
 * <br><br>
 * This class is thread-safe so long as the delegate {@link MatlabInteractor} is thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class LoggingMatlabInteractor<E> implements MatlabInteractor<E>
{
    private static final Logger LOGGER = Logger.getLogger(LoggingMatlabInteractor.class.getName());
    static
    {
        LOGGER.setLevel(Level.FINER);
    }
    
    private final MatlabInteractor<E> _delegateInteractor;
    private final boolean _exploreArrays;
    
    /**
     * Constructs the interactor. If (@code exploreArrays} is {@code true} then when the returned value is an array
     * it will be recursively explored and the complete contents of that array will be logged. Exploring very large
     * arrays can be considerably expensive.
     * 
     * @param interactor 
     * @param exploreArrays 
     */
    public LoggingMatlabInteractor(MatlabInteractor<E> interactor, boolean exploreArrays)
    {
        _delegateInteractor = interactor;
        _exploreArrays = exploreArrays;
    }
    
    /**
     * Configures the {@code ConsoleHandler} responsible for showing logging records to show the records that are
     * logged by this interactor. This is behavior is useful if you have not otherwise configured logging in your
     * application.
     */
    public static void showInConsoleHandler()
    {
        for(Handler handler : Logger.getLogger("").getHandlers())
        {
            if(handler instanceof ConsoleHandler)
            {
                handler.setLevel(Level.FINER);
            }
        }
    }
    
    private static abstract class VoidInvocation
    {
        private final Object[] _args;
        public VoidInvocation(Object... args)
        {
            _args = args;
        }
        
        public Object[] getArgs() { return _args; }
        public abstract void invoke() throws MatlabInvocationException;
        public abstract String getName();
    }
    
    private static abstract class ReturningInvocation<T>
    {        
        private final Object[] _args;
        public ReturningInvocation(Object... args)
        {
            _args = args;
        }
        
        public Object[] getArgs() { return _args; }
        public abstract T invoke() throws MatlabInvocationException;
        public abstract String getName();
    }

    private void invoke(VoidInvocation invocation) throws MatlabInvocationException
    {   
        LOGGER.entering(this.getClass().getName(), invocation.getName(), invocation.getArgs());
        
        try
        {
            invocation.invoke();
            LOGGER.exiting(this.getClass().getName(), invocation.getName());
        }
        catch(MatlabInvocationException e)
        {            
            LOGGER.throwing(this.getClass().getName(), invocation.getName(), e);
            LOGGER.exiting(this.getClass().getName(), invocation.getName());
            
            throw e;
        }
    }

    private <T> T invoke(ReturningInvocation<T> invocation) throws MatlabInvocationException
    {
        T data;
        
        LOGGER.entering(this.getClass().getName(), invocation.getName(), invocation.getArgs());
        try
        {
            data = invocation.invoke();
            LOGGER.exiting(this.getClass().getName(), invocation.getName(), formatResult(data));
            
        }
        catch(MatlabInvocationException e)
        {
            LOGGER.throwing(this.getClass().getName(), invocation.getName(), e);
            LOGGER.exiting(this.getClass().getName(), invocation.getName());
            
            throw e;
        }
        return data;
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param command
     * @throws MatlabInvocationException 
     */
    @Override
    public void eval(final String command) throws MatlabInvocationException
    {           
        this.invoke(new VoidInvocation(command)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateInteractor.eval(command);
            }

            @Override
            public String getName()
            {
                return "eval(String)";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param command
     * @param returnCount
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>(command, returnCount)
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateInteractor.returningEval(command, returnCount);
            }

            @Override
            public String getName()
            {
                return "returningEval(String, int)";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param functionName
     * @param args
     * @throws MatlabInvocationException 
     */
    @Override
    public void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        this.invoke(new VoidInvocation(functionName, args)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateInteractor.feval(functionName, args);
            }

            @Override
            public String getName()
            {
                return "feval(String, Object[])";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param functionName
     * @param args
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>(functionName, args)
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateInteractor.returningFeval(functionName, args);
            }

            @Override
            public String getName()
            {
                return "returningFeval(String, Object[])";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
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
        return this.invoke(new ReturningInvocation<E>(functionName, args, returnCount)
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateInteractor.returningFeval(functionName, args, returnCount);
            }

            @Override
            public String getName()
            {
                return "returningFeval(String, Object[], int)";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException 
     */
    @Override
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {   
        this.invoke(new VoidInvocation(variableName, value)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _delegateInteractor.setVariable(variableName, value);
            }

            @Override
            public String getName()
            {
                return "setVariable(String, int)";
            }
        });
    }

    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param variableName
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public E getVariable(final String variableName) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<E>(variableName)
        {
            @Override
            public E invoke() throws MatlabInvocationException
            {
                return _delegateInteractor.getVariable(variableName);
            }

            @Override
            public String getName()
            {
                return "getVariable(String)";
            }
        });
    }
    
    /**
     * Delegates to the interactor; logs the interaction.
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public <T> T invokeAndWait(final MatlabThreadCallable<T> callable) throws MatlabInvocationException
    {
        return this.invoke(new ReturningInvocation<T>(callable)
        {
            @Override
            public T invoke() throws MatlabInvocationException
            {
                return _delegateInteractor.invokeAndWait(callable);
            }

            @Override
            public String getName()
            {
                return "invokeAndWait(MatlabThreadCallable)";
            }
        });
    }
    
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() +
                " delegate=" + _delegateInteractor + "," +
                " exploreArrays=" + _exploreArrays + "]";
    }
    
    private String formatResult(Object result)
    {
        String formattedResult;
        
        if(result == null)
        {
            formattedResult = "null";
        }
        else if(result.getClass().isArray())
        {
            if(_exploreArrays)
            {
                formattedResult = result.getClass().getName() + "\n" + formatResult(result, 0).trim();
            }
            else
            {
                formattedResult = result.toString();
            }
        }
        else
        {
            formattedResult = result.getClass().getName() + ": " + result.toString();
        }
        
        return formattedResult;
    }
    
    /**
     * Takes in the result from MATLAB and turns it into an easily readable format.
     * 
     * @param result
     * @param level, pass in 0 to initialize, used recursively
     * @return description
     */
    private static String formatResult(Object result, int level)
    {
        //Message builder
        StringBuilder builder = new StringBuilder();
        
        //Tab offset for levels
        String tab = "";
        for(int i = 0; i < level + 1; i++)
        {
            tab += "  ";
        }
        
        //If the result is null
        if(result == null)
        {
            builder.append("null\n");
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
                
                builder.append(componentName);
                builder.append(" array, length = ");
                builder.append(length);
                builder.append("\n");
                
                for(int i = 0; i < length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(componentName);
                    builder.append(": ");
                    builder.append(Array.get(result, i));
                    builder.append("\n");
                }
            }
            //Object array
            else
            {
                Object[] array = (Object[]) result;
                
                builder.append("Object array, length = ");
                builder.append(array.length);
                builder.append("\n");
                
                for(int i = 0; i < array.length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(formatResult(array[i], level + 1));
                }
            }
        }
        //If an Object and not an array
        else
        {   
            builder.append(result.getClass().getCanonicalName());
            builder.append(": ");
            builder.append(result);
            builder.append("\n");
        }
        
        return builder.toString();
    }
}