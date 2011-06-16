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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mathworks.jmi.Matlab;
import com.mathworks.jmi.MatlabException;
import com.mathworks.jmi.NativeMatlab;

/**
 * This code is inspired by <a href="mailto:whitehouse@virginia.edu">Kamin Whitehouse</a>'s
 * <a href="http://www.cs.virginia.edu/~whitehouse/matlab/JavaMatlab.html">MatlabControl</a>.
 * <br><br>
 * This class runs inside of the MATLAB Java Virtual Machine and relies upon the {@code jmi.jar} which is distributed
 * with MATLAB in order to send commands to MATLAB and receive results.
 * <br><br>
 * Only this class and {@link MatlabExceptionWrapper} directly interact with {@code jmi.jar}.
 *
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class JMIWrapper
{   
    /**
     * Map of variables used by {@link #getVariableValue(String)}, and {@link #setVariable(String, Object)}.
     */
    private static final Map<String, Object> VARIABLES = new HashMap<String, Object>();
    
    /**
     * Map of unique identifier to stored object.
     */
    private static final Map<String, StoredObject> STORED_OBJECTS = new HashMap<String, StoredObject>();
    
    /**
     * The name of this class and package.
     */
    private static final String CLASS_NAME = JMIWrapper.class.getName();
    
    /**
     * Gets the variable value stored by {@link #setVariable(String, Object)}. Removes the reference; it cannot be
     * retrieved again.
     * 
     * @param variableName
     * 
     * @return variable value
     */
    public static Object retrieveVariableValue(String variableName)
    {
        Object result = VARIABLES.get(variableName);
        VARIABLES.remove(variableName);
        
        return result;
    }
    
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
    synchronized String storeObject(Object obj, boolean storePermanently)
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
        private final Object object;
        private final boolean storePermanently;
        private final String id;
        
        private StoredObject(Object object, boolean storePermanently)
        {
            this.object = object;
            this.storePermanently = storePermanently;
            this.id = "STORED_OBJECT_" + UUID.randomUUID().toString();
        }
    }
    
    /**
     * Sets the variable to the given value. This is done by storing the variable in Java and then retrieving it in
     * MATLAB by calling a Java method that will return it.
     * 
     * @param variableName
     * @param value
     * 
     * @throws MatlabInvocationException
     */
    synchronized void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        VARIABLES.put(variableName, value);
        this.eval(variableName + " = " + CLASS_NAME + ".retrieveVariableValue('" + variableName + "');");
    }
    
    /**
     * Convenience method to retrieve a variable's value from MATLAB.
     * 
     * @param variableName
     * 
     * @throws MatlabInvocationException
     */
    synchronized Object getVariable(String variableName) throws MatlabInvocationException
    {
        return this.returningEval(variableName, 1);
    }
    
    /**
     * @see MatlabInteractor#exit()
     */
    synchronized void exit() throws MatlabInvocationException
    {
        new Thread()
        {
            @Override
            public void run()
            {
                Matlab.whenMatlabReady(new Runnable()
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
        }.start();
    }
    
    /**
     * @see MatlabInteractor#eval(java.lang.String)
     */
    synchronized void eval(final String command) throws MatlabInvocationException
    {   
        this.returningEval(command, 0);
    }
    
    /**
     * @see MatlabInteractor#returningEval(java.lang.String, int)
     */
    synchronized Object returningEval(final String command, final int returnCount) throws MatlabInvocationException
    {
        return this.returningFeval("eval", new Object[]{ command }, returnCount);
    }

    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[])
     */
    synchronized void feval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        this.returningFeval(functionName, args, 0);
    }

    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[], int)
     */
    synchronized Object returningFeval(final String functionName, final Object[] args,
            final int returnCount) throws MatlabInvocationException
    {   
        if(EventQueue.isDispatchThread())
        {
            throw new MatlabInvocationException(MatlabInvocationException.EVENT_DISPATCH_THREAD_MSG);
        }
        else if(NativeMatlab.nativeIsMatlabThread())
        {
            try
            {
                return Matlab.mtFevalConsoleOutput(functionName, args, returnCount);
            }
            catch(Exception e)
            {
                throw new MatlabInvocationException(MatlabInvocationException.INTERNAL_EXCEPTION_MSG, e);
            }
        }
        else
        {
            final MatlabReturn matlabReturn = new MatlabReturn();
            
            Matlab.whenMatlabReady(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        matlabReturn.returnValue = Matlab.mtFevalConsoleOutput(functionName, args, returnCount);
                    }
                    catch(MatlabException e)
                    {
                        matlabReturn.thrownException = e;
                    }
                    catch(Exception e)
                    {
                        matlabReturn.returnValue = null;
                    }
                    
                    matlabReturn.returnOccurred = true;
                }    
            });
            
            //Wait for MATLAB's main thread to finish computation
            while(!matlabReturn.returnOccurred);
            
            if(matlabReturn.thrownException != null)
            {
                Exception cause = new MatlabExceptionWrapper(matlabReturn.thrownException);
                throw new MatlabInvocationException(MatlabInvocationException.INTERNAL_EXCEPTION_MSG, cause);
            }
            else
            {
                return matlabReturn.returnValue;
            }
        }
    }
    
    /**
     * Data returned from MATLAB.
     */
    private static class MatlabReturn
    {
        public volatile Object returnValue;
        public volatile MatlabException thrownException;
        public volatile boolean returnOccurred = false;
    }
    
    /**
     * @see MatlabInteractor#returningFeval(java.lang.String, java.lang.Object[])
     */
    synchronized Object returningFeval(final String functionName, final Object[] args) throws MatlabInvocationException
    {
        //Get the number of arguments that will be returned
        Object result = this.returningFeval("nargout", new String[] { functionName }, 1);
        int nargout = 0;
        try
        {
            nargout = (int) ((double[]) result)[0];
            
            //If an unlimited number of arguments (represented by -1), choose 1
            if(nargout == -1)
            {
                nargout = 1;
            }
        }
        catch(Exception e) {}
        
        //Send the request
        return this.returningFeval(functionName, args, nargout);
    }

    /**
     * @see MatlabInteractor#setDiagnosticMode(boolean)
     */
    synchronized void setDiagnosticMode(final boolean enable) throws MatlabInvocationException
    {
        if(EventQueue.isDispatchThread())
        {
            throw new MatlabInvocationException(MatlabInvocationException.EVENT_DISPATCH_THREAD_MSG);
        }
        else if(NativeMatlab.nativeIsMatlabThread())
        {
            Matlab.setEchoEval(enable);
        }
        else
        {
            Matlab.whenMatlabReady(new Runnable()
            {
                @Override
                public void run()
                {
                    Matlab.setEchoEval(enable);
                }
            });
        }
    }
}