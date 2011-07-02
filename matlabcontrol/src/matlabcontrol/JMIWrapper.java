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

import java.awt.EventQueue;
import java.util.concurrent.ArrayBlockingQueue;

import com.mathworks.jmi.Matlab;
import com.mathworks.jmi.NativeMatlab;

import matlabcontrol.MatlabInteractor.MatlabCallable;

/**
 * Interacts with MATLAB via the undocumented Java MATLAB Interface (JMI).
 * <br><br>
 * This code is inspired by <a href="mailto:whitehouse@virginia.edu">Kamin Whitehouse</a>'s
 * <a href="http://www.cs.virginia.edu/~whitehouse/matlab/JavaMatlab.html">MatlabControl</a>. Fixes to concurrency
 * bugs in this class have been aided by the feedback of several matlabcontrol users, thank you for your feedback!
 * <br><br>
 * This class runs inside of MATLAB's Java Virtual Machine and relies upon the Java MATLAB Interface which is
 * distributed by MathWorks as {@code jmi.jar}. It allows for Java to send {@code eval} and {@code feval} statements to
 * MATLAB and receive results. {@code jmi.jar} is <b>not</b> distributed with matlabcontrol as it is the property of
 * MathWorks. If you wish to compile the source code you will need to reference the version of {@code jmi.jar} that is
 * distributed with your copy of MATLAB. It is located at {@code matlabroot/java/jar/jmi.jar} where {@code matlabroot}
 * is the location of your MATLAB installation. The location of {@code matlabroot} can be determined by executing the
 * {@code matlabroot} command in the MATLAB Command Window.
 * <br><br>
 * This is the only class in matlabcontrol which directly links against code in {@code jmi.jar}. (And therefore also the
 * only class that needs {@code jmi.jar} to be on the classpath in order to compile.) {@link Configuration} also uses
 * code in {@code jmi.jar} but uses reflection to interact with it.
 *
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class JMIWrapper
{
    private static final MatlabThreadInteractor THREAD_INTERACTOR = new MatlabThreadInteractor();
     
    private JMIWrapper() { }
    
    /**
     * Exits MATLAB without waiting for MATLAB to return, because MATLAB will not return when exiting.
     * 
     * @throws MatlabInvocationException 
     */
    static void exit()
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Matlab.mtFevalConsoleOutput("exit", null, 0);
                }
                //This should never fail, and if it does there is no way to consistently report it back to the caller
                //because this method does not block
                catch(Exception e) { }
            }
        };
        
        if(NativeMatlab.nativeIsMatlabThread())
        {
            runnable.run();
        }
        else
        {
            Matlab.whenMatlabReady(runnable);
        }
    }
    
    //The following functions wait for MATLAB to complete the computation before returning
    //See MatlabProxy for the method documentation, acts as if running inside MATLAB
    //(A LocalMatlabProxy is just a thin wrapper around these methods)
    
    static void setVariable(final String variableName, final Object value)
    {
        invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                proxy.setVariable(variableName, value);
                
                return null;
            }
        });
    }
    
    static Object getVariable(final String variableName)
    {
        return invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                return proxy.getVariable(variableName);
            }
        });
    }
    
    static void eval(final String command)
    {           
        invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                proxy.eval(command);
                
                return null;
            }
        });
    }
    
    static Object returningEval(final String command, final int returnCount)
    {
        return invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                return proxy.returningEval(command, returnCount);
            }
        });
    }

    static void feval(final String functionName, final Object[] args)
    {   
        invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                proxy.feval(functionName, args);
                
                return null;
            }
        });
    }

    static Object returningFeval(final String functionName, final Object[] args, final int returnCount)
    {
        return invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                return proxy.returningFeval(functionName, args, returnCount);
            }      
        });
    }

    static Object returningFeval(final String functionName, final Object[] args)
    {
        return invokeAndWait(new MatlabCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy)
            {
                return proxy.returningFeval(functionName, args);
            }      
        });
    }
    
    /**
     * Invokes the {@code callable} on the main MATLAB thread and waits for the computation to be completed.
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws MatlabInvocationException 
     */
    static <T> T invokeAndWait(final MatlabCallable<T> callable)
    {
        T result;
        
        if(EventQueue.isDispatchThread())
        {
            throw MatlabInvocationException.Reason.EVENT_DISPATCH_THREAD.asException();
        }
        else if(NativeMatlab.nativeIsMatlabThread())
        {
            try
            {
                result = callable.call(THREAD_INTERACTOR);
            }
            catch(RuntimeException e)
            {
                ThrowableWrapper cause = new ThrowableWrapper(e);
                throw MatlabInvocationException.Reason.RUNTIME_CALLABLE.asException(cause);
            }
        }
        else
        {
            //Used to block the calling thread while waiting for MATLAB to finish computing
            final ArrayBlockingQueue<MatlabReturn<T>> returnQueue = new ArrayBlockingQueue<MatlabReturn<T>>(1); 

            Matlab.whenMatlabReady(new Runnable()
            {
                @Override
                public void run()
                {
                    MatlabReturn matlabReturn;
                    
                    try
                    {
                        matlabReturn = new MatlabReturn<T>(callable.call(THREAD_INTERACTOR));
                    }
                    catch(MatlabInvocationException e)
                    {
                        matlabReturn = new MatlabReturn<T>(e);
                    }
                    catch(RuntimeException e)
                    {
                        ThrowableWrapper cause = new ThrowableWrapper(e);
                        MatlabInvocationException userCausedException =
                                MatlabInvocationException.Reason.RUNTIME_CALLABLE.asException(cause);
                        matlabReturn = new MatlabReturn<T>(userCausedException);
                    }
                    
                    returnQueue.add(matlabReturn);
                }
            });

            try
            {
                //Wait for MATLAB's main thread to finish computation
                MatlabReturn<T> matlabReturn = returnQueue.take();

                //If exception was thrown, rethrow it
                if(matlabReturn.exception != null)
                {
                    throw matlabReturn.exception;
                }
                //Return data computed by MATLAB
                else
                {
                    result = matlabReturn.data;
                }
            }
            catch(InterruptedException e)
            {
                throw MatlabInvocationException.Reason.INTERRRUPTED.asException(e);
            }
        }
        
        return result;
    }
    
    /**
     * Data returned from MATLAB or exception thrown. The two different constructors are needed as opposed to using
     * {@code instanceof} because it is possible the user would want to <strong>return</strong> an exception. The
     * appropriate constructor will always be used because determining which overloaded constructor (or method) is done
     * at compile time, not run time.
     */
    private static class MatlabReturn<T>
    {
        private final T data;
        private final MatlabInvocationException exception;
        
        MatlabReturn(T value)
        {
            this.data = value;
            this.exception = null;
        }
        
        MatlabReturn(MatlabInvocationException exception)
        {
            this.data = null;
            this.exception = exception;
        }
    }
    /**
     * Interacts with MATLAB on MATLAB's main thread. Interacting on MATLAB's main thread is not enforced by this class,
     * that is done by its use in {@link JMIWrapper#invokeAndWait(matlabcontrol.MatlabProxy.MatlabCallable)}.
     */
    private static class MatlabThreadInteractor implements MatlabInteractor<Object>
    {   
        @Override
        public void setVariable(String variableName, Object value)
        {
            //TEST
            //this.returningFeval("assignin", new Object[]{ "base", variableName, new ValueBox(new Object()) }, 0);
            //this.returningFeval("eval", new Object[] { variableName + " = " + variableName + ".value;" }, 0); 
            //END-TEST
            
            
            this.returningFeval("assignin", new Object[]{ "base", variableName, value }, 0);
        }

        @Override
        public Object getVariable(String variableName)
        {
            return this.returningFeval("eval", new Object[] { variableName }, 1);
        }

        @Override
        public void eval(String command)
        {
            this.returningFeval("eval", new Object[]{ command }, 0);
        }

        @Override
        public Object returningEval(String command, int returnCount)
        {
            return this.returningFeval("eval", new Object[]{ command }, returnCount);
        }

        @Override
        public void feval(String functionName, Object[] args)
        {
            this.returningFeval(functionName, args, 0);
        }
        
        @Override
        public Object returningFeval(String functionName, Object[] args)
        {
            //Get the number of arguments that will be returned
            int nargout = 0;
            Object result = this.returningFeval("nargout", new String[] { functionName }, 1);
            
            try
            {
                nargout = (int) ((double[]) result)[0];
            }
            catch(Exception e)
            {
                throw MatlabInvocationException.Reason.AUTO_FEVAL_FAILURE.asException("unable to parse nargout info", e);
            }

            //If a variable number of return arguments (represented by -1), throw an exception
            if(nargout == -1)
            {
                String msg = functionName + " has a variable number of return  arguments. Instead use "
                        + "returningFeval(String, Object[], int) with the return count  specified.";
                throw MatlabInvocationException.Reason.AUTO_FEVAL_FAILURE.asException(msg);
            }

            return this.returningFeval(functionName, args, nargout);
        }

        @Override
        public Object returningFeval(String functionName, Object[] args, int returnCount)
        {
            try
            {
                return Matlab.mtFevalConsoleOutput(functionName, args, returnCount);
            }
            catch(Exception ex)
            {
                throw MatlabInvocationException.Reason.INTERNAL_EXCEPTION.asException(new ThrowableWrapper(ex));
            }
        }

        @Override
        public <T> T invokeAndWait(MatlabCallable<T> callable)
        {
            //Invoking this from inside a MatlabCallable does not make a lot of sense, but it can be done without
            //issue by invoking it right away as this is code is running on the MATLAB thread
            return callable.call(this);
        }
    }
}