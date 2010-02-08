package matlab;

import com.mathworks.jmi.Matlab;
import com.mathworks.jmi.MatlabListener;

/**
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

/**
 * This code is adapted from Kamin Whitehouse's MatlabControl.java.
 *
 * This class runs inside of the MATLAB jvm and uses the jmi.jar distributed
 * with MATLAB to send commands to MATLAB.
 *
 * @author <a href="mailto:whitehouse@virginia.edu">Kamin Whitehouse</a>
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabController
{
	/**
	 * This instance variable is part of MathWorks jmi.jar and allows control
	 * of a MATLAB instance
	 */
    private Matlab _matlab;
    
    /**
     * The return value from blockingFeval(...)
     */
    private Object _returnVal;

    /**
     * Creates the control. 
     */
    public MatlabController()
    {
        try
        {
            _matlab = new Matlab();
        }
        catch (Exception e)
        {
        	System.out.println(e.toString());
        }
    }

    /**
     * Evaluate a string, MATLAB script, or MATLAB function
     * 
     * @param command
     */
    public void eval(String command)
    {
        Matlab.whenMatlabReady(new MatlabEvalCommand(command));
    }

    /**
     * Evaluate a MATLAB function that requires arguments. Each element of
     * the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     */
    public void feval(String command, Object[] args)
    {
        Matlab.whenMatlabReady(new MatlabFevalCommand(command, args));
    }

    /**
     * Evaluate a MATLAB function that requires arguments and provide return arg.
     * Each element of the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     */
    public Object blockingFeval(String command, Object[] args) throws InterruptedException
    {
        _returnVal = "noReturnValYet";
        Matlab.whenMatlabReady(new MatlabBlockingFevalCommand(command, args));
        
        if (_returnVal.equals("noReturnValYet"))
        {
            synchronized(_returnVal)
            {
                 _returnVal.wait();
            }
        }
        
        return _returnVal;
    }

    /**
     * Echoing the eval statement is useful if you want to see in
     * MATLAB each time that a java function tries to execute a MATLAB
     * command.
     * 
     * @param echo
     */
    public void setEchoEval(boolean echo)
    {    			
    	Matlab.setEchoEval(echo);
    }

    /**
     * Sets the return value from a blocking feval command. When this value is
     * set it resumes the thread, allowing the value to be returned.
     * 
     * @see MatlabController#blockingFeval(String, Object[])
     * @see MatlabController.MatlabBlockingFevalCommand
     * 
     * @param val
     */
    private void setReturnVal(Object val)
    {
        synchronized(_returnVal)
        {
            Object oldVal = _returnVal;
            _returnVal = val;
            oldVal.notifyAll();
        }
    }

    /**
     * This class is used to execute a string in MATLAB
     */
    private class MatlabEvalCommand implements Runnable
    {
        protected String _command;
        
        public MatlabEvalCommand(String command)
        {
            _command = command;
        }
        
        public void run()
        {
            try
            {
            	_matlab.evalConsoleOutput(_command);
            }
            catch (Exception e) { }
        }
    }

    /**
     * This class is used to execute a function in MATLAB and pass parameters
     */
    private class MatlabFevalCommand implements Runnable
    {
    	private String _command;
    	private Object[] _args;
    	
        public MatlabFevalCommand(String command, Object[] args)
        {
            _command = command;
            _args = args;
        }        
        public void run()
        {
            try
            {
                _matlab.fevalConsoleOutput(_command, _args, 0, (MatlabListener) null);
            }
            catch (Exception e) { }
        }
    }

    /** 
     * This class is used to execute a function in MATLAB and pass parameters
     * and it also return arguments
     */
    private class MatlabBlockingFevalCommand implements Runnable
    {
    	private String _command;
    	private Object[] _args;
    	
        public MatlabBlockingFevalCommand(String command, Object[] args)
        {
        	_command = command;
        	_args = args;
        }

        public void run()
        {
            try
            {
            	MatlabController.this.setReturnVal(Matlab.mtFevalConsoleOutput(_command, _args, 0));
            }
            catch (Exception e)
            {
            	MatlabController.this.setReturnVal(null);
            }
        }
    }
}