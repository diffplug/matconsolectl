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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.mathworks.jmi.Matlab;
import com.mathworks.jmi.NativeMatlab;

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
    /**
     * Map of unique identifier to stored object.
     */
    private static final ConcurrentHashMap<String, StoredObject> STORED_OBJECTS =
            new ConcurrentHashMap<String, StoredObject>();
    
    /**
     * The fully qualified name of this class.
     */
    private static final String CLASS_NAME = JMIWrapper.class.getName();
    
    private JMIWrapper() { }
    
    /**
     * Retrieves the stored object. If it is not to be kept permanently then the reference will no longer be kept.
     * 
     * @param id
     * @return 
     */
    public static Object retrieveStoredObject(String id)
    {
        Object obj = null;
        
        StoredObject stored = STORED_OBJECTS.get(id);
        if(stored != null)
        {
            obj = stored.object;
            
            if(!stored.storePermanently)
            {
                STORED_OBJECTS.remove(id);
            }
        }
                
        return obj;
    }
    
    /**
     * @see MatlabInteractor#storeObject(java.lang.Object, boolean) 
     */
    static String storeObject(Object obj, boolean storePermanently)
    {
        StoredObject stored = new StoredObject(obj, storePermanently);
        STORED_OBJECTS.put(stored.id, stored);
        String retrievalString = CLASS_NAME + ".retrieveStoredObject('" + stored.id + "')";
        
        return retrievalString;
    }
    
    /**
     * An object stored by matlabcontrol that can be accessed via {@link #retrieveStoredObject(java.lang.String)}.
     */
    private static class StoredObject
    {
        private static AtomicLong _creationCounter = new AtomicLong();
        
        private final Object object;
        private final boolean storePermanently;
        private final String id;
        
        private StoredObject(Object object, boolean storePermanently)
        {
            this.object = object;
            this.storePermanently = storePermanently;
            this.id = "STORED_OBJECT_" + _creationCounter.incrementAndGet();
        }
    }
    
    /**
     * @see MatlabInteractor#exit()
     */
    static void exit() throws MatlabInvocationException
    {
        invoke(new Runnable()
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
        });
    }
    
    /**
     * @see MatlabInteractor#setVariable(java.lang.String, java.lang.Object) 
     */
    static void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        feval("assignin", new Object[] { "base", variableName, value });
    }
    
    /**
     * @see MatlabInteractor#getVariable(java.lang.String) 
     */
    static Object getVariable(String variableName) throws MatlabInvocationException
    {
        return returningEval(variableName, 1);
    }
    
    /**
     * @see MatlabInteractor#eval(java.lang.String)
     */
    static void eval(final String command) throws MatlabInvocationException
    {   
        returningEval(command, 0);
    }
    
    /**
     * @see MatlabInteractor#returningEval(java.lang.String, int)
     */
    static Object returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return returningFeval("eval", new Object[]{ command }, returnCount);
    }

    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[])
     */
    static void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        returningFeval(functionName, args, 0);
    }

    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[], int)
     */
    static Object returningFeval(final String functionName, final Object[] args, final int returnCount)
            throws MatlabInvocationException
    {
        return invokeAndWait(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                return Matlab.mtFevalConsoleOutput(functionName, args, returnCount);
            }
        });
    }
        
    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[])
     */
    static Object returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        return invokeAndWait(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                //Get the number of arguments that will be returned
                int nargout = 0;
                Object result = Matlab.mtFevalConsoleOutput("nargout", new String[] { functionName }, 1);
                nargout = (int) ((double[]) result)[0];

                //If an unlimited number of arguments (represented by -1), throw an exception
                if(nargout == -1)
                {
                    throw new MatlabInvocationException(functionName + " has a variable number of return arguments. " +
                            "Instead used returningFeval(String, Object[], int) with the return count specified.");
                }
                
                return Matlab.mtFevalConsoleOutput(functionName, args, nargout);
            }
                    
        });
    }
    
    private static void invoke(final Runnable runnable)
    {
        if(NativeMatlab.nativeIsMatlabThread())
        {
            runnable.run();
        }
        else
        {
            Matlab.whenMatlabReady(runnable);
        }
    }

    private static <T> T invokeAndWait(final Callable<T> callable) throws MatlabInvocationException
    {
        T result;
        
        if(EventQueue.isDispatchThread())
        {
            throw new MatlabInvocationException(MatlabInvocationException.EVENT_DISPATCH_THREAD_MSG);
        }
        else if(NativeMatlab.nativeIsMatlabThread())
        {
            try
            {
                result = callable.call();
            }
            catch(Exception e)
            {
                throw new MatlabInvocationException(MatlabInvocationException.INTERNAL_EXCEPTION_MSG, e);
            }
        }
        else
        {
            final ArrayBlockingQueue<MatlabReturn<T>> returnQueue = new ArrayBlockingQueue<MatlabReturn<T>>(1); 

            Matlab.whenMatlabReady(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        returnQueue.add(new MatlabReturn<T>(callable.call()));
                    }
                    catch(Exception e)
                    {
                        returnQueue.add(new MatlabReturn<T>(e));
                    }
                }    
            });

            try
            {
                //Wait for MATLAB's main thread to finish computation
                MatlabReturn<T> matlabReturn = returnQueue.take();

                //If exception was thrown, rethrow it
                if(matlabReturn.exceptionThrown)
                {
                    if(matlabReturn.exception instanceof MatlabInvocationException)
                    {
                        throw (MatlabInvocationException) matlabReturn.exception;
                    }
                    else
                    {
                        Throwable cause = new ThrowableWrapper(matlabReturn.exception);
                        throw new MatlabInvocationException(MatlabInvocationException.INTERNAL_EXCEPTION_MSG, cause);
                    }
                }
                //Return value computed by MATLAB
                else
                {
                    result = matlabReturn.value;
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
     * Data returned from MATLAB.
     */
    private static class MatlabReturn<T>
    {
        private final boolean exceptionThrown;
        private final T value;
        private final Exception exception;
        
        MatlabReturn(T value)
        {
            this.exceptionThrown = false;
            
            this.value = value;
            this.exception = null;
        }
        
        MatlabReturn(Exception exception)
        {
            this.exceptionThrown = true;
            
            this.value = null;
            this.exception = exception;
        }
    }
}