package example.remote;

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

/**
 * Represents a method in the matlabcontrol API along with
 * information to adjust the GUI.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MethodDescriptor
{
	/**
	 * Method signature
	 */
	public final String signature;
	
	/**
	 * Description of method.
	 */
	public final String message;
	
	/**
	 * Text that surrounds the border of the text input field.
	 */
	public final String stringInputName;
	
	/**
	 * Text that surrounds the border of the arguments input field.
	 */
	public final String argsInputName;
	
	/**
	 * Whether the return count field is enabled.
	 */
	public final boolean returnCountEnabled;
	
	/**
	 * Number of entries enabled in the argument input field.
	 */
	public final int argsInputNumberEnabled;
	
	public MethodDescriptor(String signature, String message,
					 String stringInputName, String argsInputName,
					 boolean returnCountEnabled, int argsInputNumberEnabled)
	{
		this.signature = signature;
		this.message = message;
		
		this.stringInputName = stringInputName;
		this.argsInputName = argsInputName;
		
		this.returnCountEnabled = returnCountEnabled;
		this.argsInputNumberEnabled = argsInputNumberEnabled;
	}
	
	/**
	 * Return signature as the description so that this is what will
	 * appear in the drop down list.
	 */
	public String toString()
	{
		return signature;
	}
	
	/**
	 * Descriptions and signatures of the methods shown off in the demo.
	 */
	private static final String	
	EVAL_MSG =
	"Evaluates a command in MATLAB. The result of this command will not be returned. \n\n" +
    "This is equivalent to MATLAB's eval(['command here']).",
    
    EVAL_SIGNATURE =
    "eval(String command)",
	
	RETURNING_EVAL_MSG = 
	"Evaluates a command in MATLAB. The result of this command can be returned.\n\n" +
	"This is equivalent to MATLAB's eval(['command']).\n\n" +
	"In order for the result of this command to be returned the " +
	"number of arguments to be returned must be specified by " +
	"returnCount. If the command you are evaluating is a MATLAB " +
	"function you can determine the amount of arguments it returns by using " +
	"the nargout function in the MATLAB Command Window. If it " +
	"returns -1 that means the function returns a variable number of " +
	"arguments based on what you pass in. In that case, you will need to " +
	"manually determine the number of arguments returned. If the number of " +
	"arguments returned differs from returnCount then either " +
	"null or an empty String will be returned.",
	
	RETURNING_EVAL_SIGNATURE =
	"returningEval(String command, int returnCount)",
	
	FEVAL_MSG =
	"Calls a MATLAB function with the name functionName. " +
	"Arguments to the function may be provided as args, if you " +
	"wish to call the function with no arguments pass in null. " +
	"The result of this command will not be returned.\n\n" +
	"The Objects in the array will be converted into MATLAB " + 
	"equivalents as appropriate. Importantly, this means that any " + 
	"String will be converted to a MATLAB char array, not a " + 
	"variable name.",
	
	FEVAL_SIGNATURE = 
	"feval(String functionName, Object[] args)",
			
    RETURNING_AUTO_FEVAL_MSG = 
    "Calls a MATLAB function with the name functionName. " + 
    "Arguments to the function may be provided as args, if you " +
    "wish to call the function with no arguments pass in null.\n\n" + 
    "The Objects in the array will be converted into MATLAB " +
    "equivalents as appropriate. Importantly, this means that any " +
    "String will be converted to a MATLAB char array, not a variable name. \n\n" + 
    "The result of this function can be returned. In order for a function's " + 
    "return data to be returned to MATLAB it is necessary to know how many " +
    "arguments will be returned. This method will attempt to determine that " +
    "automatically, but in the case where a function has a variable number of " + 
    "arguments returned it will only return one of them. To have all of them " + 
    "returned use returningFeval(String, Object[], int) and specify " +
    "the number of arguments that will be returned.",
    
    RETURNING_AUTO_FEVAL_SIGNATURE = 
    "returningFeval(String functionName, Object[] args)",
    
    RETURNING_FEVAL_MSG =
    "Calls a MATLAB function with the name functionName. " + 
    "Arguments to the function may be provided as args, if you " +
    "wish to call the function with no arguments pass in null.\n\n" +
    "The Objects in the array will be converted into MATLAB " +
    "equivalents as appropriate. Importantly, this means that any " +
    "String will be converted to a MATLAB char array, not a " +
    "variable name.\n\n" +
    "The result of this function can be returned. In order for the result of " +
    "this function to be returned the number of arguments to be returned must " +
    "be specified by returnCount. You can use the " +
    "nargout function in the MATLAB Command Window to determine " +
    "the number of arguments that will be returned. If nargout " +
    "returns -1 that means the function returns a variable number of " +
    "arguments based on what you pass in. In that case, you will need to " +
    "manually determine the number of arguments returned. If the number of " +
    "arguments returned differs from returnCount then either " +
    "only some of the items will be returned or null will be " +
    "returned.",
    
    RETURNING_FEVAL_SIGNATURE = 
    "returningFeval(String functionName, Object[] args, int returnCount)",
    
    GET_VARIABLE_MSG =
    "Gets the value of the variable named variableName from MATLAB.",
    
    GET_VARIABLE_SIGNATURE = 
    "getVariable(String variableName)",
    
    SET_VARIABLE_MSG = 
    "Sets the variable to the given value.",
    
    SET_VARIABLE_SIGNATURE = 
    "setVariable(String variableName, Object value)";
	
	/**
	 * Indices of entries in the array of methods.
	 */
	public static final int EVAL_INDEX = 0,
							RETURNING_EVAL_INDEX = 1,
							FEVAL_INDEX = 2,
							RETURNING_AUTO_FEVAL_INDEX = 3,
							RETURNING_FEVAL_INDEX = 4,
							SET_VARIABLE_INDEX = 5,
							GET_VARIABLE_INDEX = 6,
							METHODS_ARRAY_SIZE = GET_VARIABLE_INDEX + 1;
	
	/**
	 * The methods in the API that are part of the demo.
	 */
	public static final MethodDescriptor[] METHODS = new MethodDescriptor[METHODS_ARRAY_SIZE];
	static
	{
		METHODS[EVAL_INDEX] = new MethodDescriptor(EVAL_SIGNATURE, EVAL_MSG, "command", "args (disabled)", false, 0);
		METHODS[RETURNING_EVAL_INDEX] = new MethodDescriptor(RETURNING_EVAL_SIGNATURE, RETURNING_EVAL_MSG, "command", "args (disabled)", true, 0);
		METHODS[FEVAL_INDEX] = new MethodDescriptor(FEVAL_SIGNATURE, FEVAL_MSG, "functionName", "args", false, ArrayPanel.NUM_ENTRIES);
		METHODS[RETURNING_AUTO_FEVAL_INDEX] = new MethodDescriptor(RETURNING_AUTO_FEVAL_SIGNATURE, RETURNING_AUTO_FEVAL_MSG, "functionName", "args", false, ArrayPanel.NUM_ENTRIES);
		METHODS[RETURNING_FEVAL_INDEX] = new MethodDescriptor(RETURNING_FEVAL_SIGNATURE, RETURNING_FEVAL_MSG, "functionName", "args", true, ArrayPanel.NUM_ENTRIES);
		METHODS[SET_VARIABLE_INDEX] = new MethodDescriptor(SET_VARIABLE_SIGNATURE, SET_VARIABLE_MSG, "variableName", "value", false, 1);
		METHODS[GET_VARIABLE_INDEX] = new MethodDescriptor(GET_VARIABLE_SIGNATURE, GET_VARIABLE_MSG, "variableName", "args (disabled)", false, 0);
	}
}