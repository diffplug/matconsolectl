package matlab;

/*
 * Copyright (c) 2010, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/* The following copyright notice is from Kamin Whitehouse's MatlabControl.java
 * 
 * "Copyright (c) 2001 and The Regents of the University
 * of California.  All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 */

import com.mathworks.jmi.CompletionObserver;
import com.mathworks.jmi.Matlab;

/**
 * This code is adapted from
 * <a href="mailto:whitehouse@virginia.edu">Kamin Whitehouse</a>'s
 * MatlabControl.java.
 *
 * This class runs inside of the MATLAB JVM and relies upon the jmi.jar
 * which is distributed with MATLAB in order to send commands to MATLAB and
 * receive results.
 *
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabController
{
	/**
	 * This instance variable is part of MathWorks jmi.jar and allows control
	 * of a MATLAB instance
	 */
    private Matlab _matlab = new Matlab();
    
    /**
     * The return value from blocking methods.
     */
    private Object _returnValue;
    
    /**
     * The default value that _returnVal is set to before an actual return
     * value is returned.
     */
    private static final String BEFORE_RETURN_VALUE = "noReturnValYet";

    /**
     * Exits MATLAB.
     */
    public void exit()
    {
    	_matlab.evalConsoleOutput("exit");
    }
    
    /**
     * Evaluates a command in MATLAB. The result of this command will not be
     * returned.
     * 
     * This is equivalent to MATLAB's <code>eval(['command here'])</code>.
     * 
     * @param command the command to be evaluated in MATLAB
     * 
     * @see #returningEval(String, int)
     */
    public void eval(final String command) throws InterruptedException
    {  
        _returnValue = BEFORE_RETURN_VALUE;
        
        Matlab.whenMatlabReady(new Runnable()
        {
            public void run()
            {
                try
                {
                	//Execute the eval command
                	//When it is completed set in motion the waking of the thread
                	_matlab.evalConsoleOutput(command, new CompletionObserver()
                	{
    					public void completed(int flag, Object result)
    					{
    						MatlabController.this.setReturnValue(null);
    					}
                	});
                }
                catch (Exception e)
                {
                	MatlabController.this.setReturnValue(null);
                }
            }
        });
        
        this.getReturnValue();
    }
    
    /**
     * Evaluates a command in MATLAB. The result of this command can be
     * returned.
     * 
     * This is equivalent to MATLAB's <code>eval(['command here'])</code>.
     * 
     * In order for the result of this command to be returned the
     * number of items to be returned must be specified by
     * <code>returnCount</code>. If the command you are evaluating is a MATLAB
     * function you can determine the amount of items it returns by using the
     * <code>nargout</code> function in the MATLAB Command Window. If it
     * returns -1 that means the function returns a variable number of
     * arguments based on what you pass in. In that case, you will need to
     * manually determine the number of items returned. If the number of items
     * returned differs from <code>returnCount</code> then an empty String will
     * be returned.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param returnCount the number of arguments that will be returned from evaluating the command
     * 
     * @see #eval(String)
     */
    public Object returningEval(final String command, final int returnCount) throws InterruptedException
    {
        _returnValue = BEFORE_RETURN_VALUE;
        Matlab.whenMatlabReady(new Runnable()
        {
        	public void run()
        	{
                try
                {
                	MatlabController.this.setReturnValue(Matlab.mtEval(command, returnCount));
                }
                catch (Exception e)
                {
                	MatlabController.this.setReturnValue(null);
                }
        	}
        });
        
        return this.getReturnValue();
    }

    /**
     * Calls a MATLAB function with the name <code>functionName</code>.
     * Arguments to the function may be provided as <code>args</code>, if you
     * wish to call the function with no arguments pass in <code>null</code>.
     * The result of this command will not be returned.
     * 
     * The <code>Object</code>s in the array will be converted into MATLAB
     * equivalents as appropriate. Importantly, this means that any String will
     * be converted to a MATLAB char array, not a variable name.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, <code>null</code> if none
     * 
     * @see #returningFeval(String, Object[], int)
     */
    public void feval(final String functionName, final Object[] args) throws InterruptedException
    {        
        _returnValue = BEFORE_RETURN_VALUE;
        Matlab.whenMatlabReady(new Runnable()
        {
            public void run()
            {
                try
                {
                	//Execute the feval command
                	//When it is completed set in motion the waking of the thread
                	_matlab.fevalConsoleOutput(functionName, args, 0, new CompletionObserver()
                	{
    					public void completed(int flag, Object result)
    					{
    						MatlabController.this.setReturnValue(null);
    					}
                	});
                }
                catch (Exception e)
                {
                	MatlabController.this.setReturnValue(null);
                }
            }
        });
        
        this.getReturnValue();
    }

    /**
     * Calls a MATLAB function with the name <code>functionName</code>.
     * Arguments to the function may be provided as <code>args</code>, if you
     * wish to call the function with no arguments pass in <code>null</code>.
     * 
     * The result of this function can be returned. In order for the result of
     * this function to be returned the number of items to be returned must be
     * specified by <code>returnCount</code>. You can use the 
     * <code>nargout</code> function in the MATLAB Command Window to determine
     * the number of items that will be returned. If <code>nargout</code>
     * returns -1 that means the function returns a variable number of
     * arguments based on what you pass in. In that case, you will need to
     * manually determine the number of items returned. If the number of items
     * returned differs from <code>returnCount</code> then <code>null</code>
     * will be returned.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, <code>null</code> if none
     * @param returnCount the number of arguments that will be returned from this function
     * 
     * @see #feval(String, Object[])
     */
    public Object returningFeval(final String functionName, final Object[] args, final int returnCount) throws InterruptedException
    {
        _returnValue = BEFORE_RETURN_VALUE;
        Matlab.whenMatlabReady(new Runnable()
        {
            public void run()
            {
                try
                {
                	MatlabController.this.setReturnValue(Matlab.mtFeval(functionName, args, returnCount));
                }
                catch (Exception e)
                {
                	MatlabController.this.setReturnValue(null);
                }
            }	
        });
        
        return this.getReturnValue();
    }

    /**
     * Allows for enabling a diagnostic mode that will show in MATLAB each time
     * a Java method that calls into MATLAB is invoked.
     * 
     * @param echo
     */
    public void setEchoEval(final boolean echo)
    {
    	Matlab.whenMatlabReady(new Runnable()
    	{
    		public void run()
    		{
    	    	Matlab.setEchoEval(echo);
    		}
    	});
    }
    
    /**
     * Returns the value returned by MATLAB to {@link #returningEval(String, int)}
     * and {@link #returningFeval(String, Object[], int)}, and returns null
     * in the cases of {@link #eval(String)} and {@link #feval(String, Object[])}.
     * 
     * Returning this operates by pausing the thread until MATLAB returns a value.
     * 
     * @return result of MATLAB call
     * 
     * @throws InterruptedException
     * 
     * @see #setReturnValue(Object)
     */
    private Object getReturnValue() throws InterruptedException
    {
        //If _returnVal has not been changed yet (in all likelihood it has not)
        //then wait, it will be resumed when the call to MATLAB returns
        if (_returnValue.equals(BEFORE_RETURN_VALUE))
        {
            synchronized(_returnValue)
            {
                 _returnValue.wait();
            }
        }
        
        return _returnValue;
    }

    /**
     * Sets the return value from any of the eval or feval commands. In the
     * case of the non-returning value null is passed in, but it still provides
     * the functionality of waking up the thread so that {@link #getReturnValue()}
     * will be able to return.
     * 
     * @param val
     * 
     * @see #getReturnValue()
     */
    private void setReturnValue(Object val)
    {
        synchronized(_returnValue)
        {
            Object oldVal = _returnValue;
            _returnValue = val;
            oldVal.notifyAll();
        }
    }
}