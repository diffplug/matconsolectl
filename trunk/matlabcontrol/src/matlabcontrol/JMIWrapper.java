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
import java.util.logging.Level;
import java.util.logging.Logger;

import matlabcontrol.MatlabProxy.MatlabThreadCallable;

/**
 * This code is inspired by <a href="mailto:whitehouse@virginia.edu">Kamin Whitehouse</a>'s
 * <a href="http://www.cs.virginia.edu/~whitehouse/matlab/JavaMatlab.html">MatlabControl</a>.
 * <br><br>
 * This class runs inside of MATLAB's Java Virtual Machine and relies upon the {@code jmi.jar} which is distributed
 * with MATLAB in order to send commands to MATLAB and receive results. This is the only class in matlabcontrol which
 * directly interacts with any code in {@code jmi.jar}.
 *
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class JMIWrapper
{
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
                catch (Exception e) { }
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
    //See MatlabInteractor for the method documentation
    
    static void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {
        invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                proxy.setVariable(variableName, value);
                
                return null;
            }
        });
    }
    
    static Object getVariable(final String variableName) throws MatlabInvocationException
    {
        return invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                return proxy.getVariable(variableName);
            }
        });
    }
    
    static void eval(final String command) throws MatlabInvocationException
    {           
        invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                proxy.eval(command);
                
                return null;
            }
        });
    }
    
    static Object returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                return proxy.returningEval(command, returnCount);
            }
        });
    }

    static void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {   
        invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                proxy.feval(functionName, args);
                
                return null;
            }
        });
    }

    static Object returningFeval(final String functionName, final Object[] args, final int returnCount)
            throws MatlabInvocationException
    {
        return invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
            {
                return proxy.returningFeval(functionName, args, returnCount);
            }      
        });
    }

    static Object returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        return invokeAndWait(new MatlabThreadCallable<Object>()
        {
            @Override
            public Object call(MatlabInteractor<Object> proxy) throws Exception
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
    static <T> T invokeAndWait(final MatlabThreadCallable<T> callable) throws MatlabInvocationException
    {
        T result;
        
        if(EventQueue.isDispatchThread())
        {
            throw new MatlabInvocationException(MatlabInvocationException.EVENT_DISPATCH_THREAD_MSG);
        }
        else if(NativeMatlab.nativeIsMatlabThread())
        {
            MatlabThreadInteractor proxy = new MatlabThreadInteractor();
            
            try
            {
                result = callable.call(proxy);
            }
            catch(Exception ex)
            {
                //If the exception was thrown by a proxy method
                if(proxy.isExceptionThrown())
                {
                    throw proxy.getThrownException();
                }
                //If the exception was thrown by user code
                else
                {
                    throw new MatlabInvocationException(MatlabInvocationException.USER_EXCEPTION_CALLABLE_MSG, ex);
                }
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
                    MatlabThreadInteractor proxy = new MatlabThreadInteractor();
                    MatlabReturn matlabReturn;
                    
                    try
                    {
                        matlabReturn = new MatlabReturn<T>(callable.call(proxy));
                    }
                    catch(Exception ex)
                    {
                        //If the exception was thrown by a proxy method
                        if(proxy.isExceptionThrown())
                        {
                            matlabReturn = new MatlabReturn<T>(proxy.getThrownException());
                        }
                        //If the exception was thrown by user code
                        else
                        {
                            MatlabInvocationException userCausedException = new MatlabInvocationException(
                                    MatlabInvocationException.USER_EXCEPTION_CALLABLE_MSG, ex);
                            matlabReturn = new MatlabReturn<T>(userCausedException);
                        }
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
                throw new MatlabInvocationException(MatlabInvocationException.INTERRUPTED_MSG, e);
            }
        }
        
        return result;
    }
    
    /**
     * Data returned from MATLAB or exception thrown. The two different constructs are needed as opposed to using
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
     * that is done by its use in {@link JMIWrapper#invokeAndWait(matlabcontrol.MatlabProxy.MatlabThreadCallable)}.
     */
    private static class MatlabThreadInteractor implements MatlabInteractor<Object>
    {   
        /**
         * Keeps track of if an exception is thrown, to be able to distinguish between an exception thrown by
         * interacting with MATLAB vs. in the rest of a MatlabThreadCallback
         */
        private MatlabInvocationException _exception = null;
        
        @Override
        public void setVariable(String variableName, Object value) throws MatlabInvocationException
        {
            this.returningFeval("assignin", new Object[]{ "base", variableName, value }, 0);
        }

        @Override
        public Object getVariable(String variableName) throws MatlabInvocationException
        {
            return this.returningFeval("eval", new Object[] { variableName }, 1);
        }

        @Override
        public void eval(String command) throws MatlabInvocationException
        {
            this.returningFeval("eval", new Object[]{ command }, 0);
        }

        @Override
        public Object returningEval(String command, int returnCount) throws MatlabInvocationException
        {
            return this.returningFeval("eval", new Object[]{ command }, returnCount);
        }

        @Override
        public void feval(String functionName, Object[] args) throws MatlabInvocationException
        {
            this.returningFeval(functionName, args, 0);
        }
        
        @Override
        public Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException
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
                _exception = new MatlabInvocationException("Unable to parse nargout information", e);
                throw _exception;
            }

            //If a variable number of return arguments (represented by -1), throw an exception
            if(nargout == -1)
            {
                _exception = new MatlabInvocationException(functionName + " has a variable number of return "
                        + "arguments. Instead use returningFeval(String, Object[], int) with the return count "
                        + "specified.");
                return _exception;
            }

            return this.returningFeval(functionName, args, nargout);
        }

        @Override
        public Object returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException
        {
            try
            {
                return Matlab.mtFevalConsoleOutput(functionName, args, returnCount);
            }
            catch(Exception ex)
            {
                Throwable cause = new ThrowableWrapper(ex);
                _exception = new MatlabInvocationException(MatlabInvocationException.INTERNAL_EXCEPTION_MSG, cause);
                throw _exception;
            }
        }

        @Override
        public <T> T invokeAndWait(MatlabThreadCallable<T> callable) throws MatlabInvocationException
        {
            //Invoking this from inside a MatlabThreadCallable does not make a lot of sense, but it can be done without
            //issue by invoking it right away as this is code is running on the MATLAB thread
            MatlabThreadInteractor interactor = new MatlabThreadInteractor();
            try
            {
                return callable.call(interactor);
            }
            catch(Exception ex)
            {
                //If the exception was thrown by a proxy method
                if(interactor.isExceptionThrown())
                {
                    _exception = interactor.getThrownException();
                }
                //If the exception was thrown by user code
                else
                {
                    _exception = new MatlabInvocationException(MatlabInvocationException.USER_EXCEPTION_CALLABLE_MSG, ex);
                }
                throw _exception;
            }
        }
        
        public boolean isExceptionThrown()
        {
            return _exception != null;
        }
        
        public MatlabInvocationException getThrownException()
        {
            return _exception;
        }
    }
}