/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

/**
 * Represents a method in the matlabcontrol API along with information to adjust the GUI.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
enum ProxyMethodDescriptor {
	EVAL("void eval(String command)",
			Documentation.EVAL, "command", "args (disabled)", false, 0), RETURNING_EVAL("Object[] returningEval(String command, int nargout)",
					Documentation.RETURNING_EVAL, "comamnd", "args (disabled)", true, 0), FEVAL("void feval(String functionName, Object... args)",
							Documentation.FEVAL, "functionName", "args", false, ArrayPanel.NUM_ENTRIES), RETURNING_FEVAL("Object[] returningFeval(String functionName, int nargout, Object... args)",
									Documentation.RETURNING_FEVAL, "functionName", "args", true, ArrayPanel.NUM_ENTRIES), SET_VARIABLE("void setVariable(String variableName, Object value)",
											Documentation.SET_VARIABLE, "variableName", "value", false, 1), GET_VARIABLE("Object getVariable(String variableName)",
													Documentation.GET_VARIABLE, "variableName", "args (disabled)", false, 0);

	private ProxyMethodDescriptor(String signature, String message, String stringInputName,
			String argsInputName, boolean returnCountEnabled, int argsInputNumberEnabled) {
		this.signature = signature;
		this.message = message;
		this.stringInputName = stringInputName;
		this.argsInputName = argsInputName;
		this.returnCountEnabled = returnCountEnabled;
		this.argsInputNumberEnabled = argsInputNumberEnabled;
	}

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

	@Override
	public String toString() {
		return signature;
	}

	private static class Documentation {
		private static final String EVAL = "<html>Evaluates a command in MATLAB. This is equivalent to MATLAB's <code>eval('command')</code>.</html>",

		RETURNING_EVAL = "<html>Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's " +
				"<code>eval('command')</code>." +
				"<br><br>" +
				"In order for the result of this command to be returned the number of arguments to be returned must be " +
				"specified by <code>nargout</code>. This is equivalent in MATLAB to the number of variables placed on the " +
				"left hand side of an expression. For example, in MATLAB the <code>inmem</code> function may be used with " +
				"either 1, 2, or 3 return values each resulting in a different behavior:" +
				"<pre><code>" +
				"M = inmem;\n" +
				"[M, X] = inmem;\n" +
				"[M, X, J] = inmem;\n" +
				"</code></pre>" +
				"The returned <code>Object</code> array will be of length <code>nargout</code> with each return argument " +
				"placed into the corresponding array position." +
				"<br><br>" +
				"If the command cannot return the number of arguments specified by <code>nargout</code> then an exception " +
				"will be thrown.</html>",

		FEVAL = "<html>Calls a MATLAB function with the name <code>functionName</code>. Arguments to the function may be " +
				"provided as <code>args</code>, but are not required if the function needs no arguments." +
				"<br><br>" +
				"The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means " +
				"that a <code>String</code> will be converted to a MATLAB <code>char</code> array, not a variable name.</html>",

		RETURNING_FEVAL = "<html>Calls a MATLAB function with the name <code>functionName</code>, returning the result. Arguments to " +
				"the function may be provided as <code>args</code>, but are not required if the function needs no arguments." +
				"<br><br>" +
				"The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means " +
				"that a <code>String</code> will be converted to a MATLAB <code>char</code> array, not a variable name." +
				"<br><br>" +
				"In order for the result of this function to be returned the number of arguments to be returned must be " +
				"specified by <code>nargout</code>. This is equivalent in MATLAB to the number of variables placed on the " +
				"left hand side of an expression. For example, in MATLAB the <code>inmem</code> function may be used with " +
				"either 1, 2, or 3 return values each resulting in a different behavior:" +
				"<pre><code>" +
				"M = inmem;\n" +
				"[M, X] = inmem;\n" +
				"[M, X, J] = inmem;\n" +
				"</code></pre>" +
				"The returned <code>Object</code> array will be of length <code>nargout</code> with each return argument " +
				"placed into the corresponding array position." +
				"<br><br>" +
				"If the function is not capable of returning the number of arguments specified by <code>nargout</code> then " +
				"an exception will be thrown.</html>",

		SET_VARIABLE = "<html>Sets <code>variableName</code> to <code>value</code> in MATLAB, creating the variable if it does not " +
				"yet exist.</html>",

		GET_VARIABLE = "<html>Gets the value of <code>variableName</code> in MATLAB.</html>";
	}
}
