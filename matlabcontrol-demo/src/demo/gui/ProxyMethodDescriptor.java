/*
 * Copyright (c) 2013, Joshua Kaplan
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
package demo.gui;

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
