/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

/**
 * Operations which interact with a session of MATLAB.
 * <br><br>
 * <b>WARNING:</b> This interface is not intended to be implemented by users of matlabcontrol. Methods may be added to
 * this interface, and these additions will not be considered breaking binary compatability.
 * 
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public interface MatlabOperations {
	/**
	 * Evaluates a command in MATLAB. This is equivalent to MATLAB's {@code eval('command')}.
	 * 
	 * @param command the command to be evaluated in MATLAB
	 * @throws MatlabInvocationException 
	 */
	public void eval(String command) throws MatlabInvocationException;

	/**
	 * Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's {@code eval('command')}.
	 * <br><br>
	 * In order for the result of this command to be returned the number of arguments to be returned must be specified
	 * by {@code nargout}. This is equivalent in MATLAB to the number of variables placed on the left hand side of an
	 * expression. For example, in MATLAB the {@code inmem} function may be used with either 1, 2, or 3 return values
	 * each resulting in a different behavior:
	 * <pre>
	 * {@code
	 * M = inmem;
	 * [M, X] = inmem;
	 * [M, X, J] = inmem;
	 * }
	 * </pre>
	 * The returned {@code Object} array will be of length {@code nargout} with each return argument placed into the
	 * corresponding array position.
	 * <br><br>
	 * If the command cannot return the number of arguments specified by {@code nargout} then an exception will be
	 * thrown.
	 * 
	 * @param command the command to be evaluated in MATLAB
	 * @param nargout the number of arguments that will be returned from evaluating {@code command}
	 * @return result of MATLAB command, the length of the array will be {@code nargout}
	 * @throws MatlabInvocationException
	 */
	public Object[] returningEval(String command, int nargout) throws MatlabInvocationException;

	/**
	 * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
	 * be provided as {@code args}, but are not required if the function needs no arguments.
	 * <br><br>
	 * The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means that a
	 * {@code String} will be converted to a MATLAB {@code char} array, not a variable name.
	 * 
	 * @param functionName the name of the MATLAB function to call
	 * @param args the arguments to the function
	 * @throws MatlabInvocationException 
	 */
	public void feval(String functionName, Object... args) throws MatlabInvocationException;

	/**
	 * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
	 * be provided as {@code args}, but are not required if the function needs no arguments.
	 * <br><br>
	 * The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means that a
	 * {@code String} will be converted to a MATLAB {@code char} array, not a variable name.
	 * <br><br>
	 * In order for the result of this function to be returned the number of arguments to be returned must be specified
	 * by {@code nargout}. This is equivalent in MATLAB to the number of variables placed on the left hand side of an
	 * expression. For example, in MATLAB the {@code inmem} function may be used with either 1, 2, or 3 return values
	 * each resulting in a different behavior:
	 * <pre>
	 * {@code
	 * M = inmem;
	 * [M, X] = inmem;
	 * [M, X, J] = inmem;
	 * }
	 * </pre>
	 * The returned {@code Object} array will be of length {@code nargout} with each return argument placed into the
	 * corresponding array position.
	 * <br><br>
	 * If the function is not capable of returning the number of arguments specified by {@code nargout} then an
	 * exception will be thrown.
	 * 
	 * @param functionName the name of the MATLAB function to call
	 * @param nargout the number of arguments that will be returned by {@code functionName}
	 * @param args the arguments to the function
	 * @return result of MATLAB function, the length of the array will be {@code nargout}
	 * @throws MatlabInvocationException 
	 */
	public Object[] returningFeval(String functionName, int nargout, Object... args) throws MatlabInvocationException;

	/**
	 * Sets {@code variableName} to {@code value} in MATLAB, creating the variable if it does not yet exist.
	 * 
	 * @param variableName
	 * @param value
	 * @throws MatlabInvocationException
	 */
	public void setVariable(String variableName, Object value) throws MatlabInvocationException;

	/**
	 * Gets the value of {@code variableName} in MATLAB.
	 * 
	 * @param variableName
	 * @return value
	 * @throws MatlabInvocationException
	 */
	public Object getVariable(String variableName) throws MatlabInvocationException;
}
