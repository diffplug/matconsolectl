/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.util.HashSet;
import java.util.Set;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabFunctionHandle extends MatlabType {
	private final String _function;

	public MatlabFunctionHandle(String functionName) {
		if (functionName == null) {
			throw new NullPointerException("MATLAB function name may not be null");
		}

		if (functionName.isEmpty()) {
			throw new IllegalArgumentException("Invalid MATLAB function name; may not be an empty String");
		}

		//Anonymous function
		if (functionName.startsWith("@")) {
			checkAnonymousFunction(functionName);
		}
		//Regular function
		else {
			checkFunctionName(functionName);
		}

		_function = functionName;
	}

	private static void checkAnonymousFunction(String function) {
		char[] functionChars = function.toCharArray();

		//Validate that the the anonymous function is of the form @(something)
		if (functionChars.length < 2 || functionChars[1] != '(') {
			throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
					"@ must be followed with (");
		}
		int closingParenIndex = function.indexOf(")");
		if (closingParenIndex == -1) {
			throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
					"Must terminate the argument list with )");
		}

		//Validate all of the arguments of the anonymous function
		String argsString = function.substring(2, closingParenIndex);
		String[] args;
		args = argsString.isEmpty() ? new String[0] : argsString.split(", ");
		Set<String> seenArgs = new HashSet<String>();
		for (String arg : args) {
			arg = arg.trim();

			if (seenArgs.contains(arg)) {
				throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
						"Invalid argument name: " + arg + "\n" +
						"Argument names must be unique");
			}
			seenArgs.add(arg);

			char[] argsChars = arg.toCharArray();

			if (!Character.isLetter(argsChars[0])) {
				throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
						"Invalid argument name: " + arg + "\n" +
						"Argument must begin with a letter");
			}

			for (char element : argsChars) {
				if (!(Character.isLetter(element) || Character.isDigit(element) || element == '_')) {
					throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
							"Invalid argument name: " + arg + "\n" +
							"Argument must consist only of letters, numbers, and underscores");
				}
			}
		}

		//Validate the function has a body, but don't actually validate the body
		String body = function.substring(closingParenIndex + 1, function.length());
		if (body.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
					"Anonymous function must have a body");
		}
	}

	private static void checkFunctionName(String functionName) {
		char[] nameChars = functionName.toCharArray();

		if (!Character.isLetter(nameChars[0])) {
			throw new IllegalArgumentException("Invalid MATLAB function name: " + functionName + "\n" +
					"Function name must begin with a letter");
		}

		for (char element : nameChars) {
			if (!(Character.isLetter(element) || Character.isDigit(element) || element == '_')) {
				throw new IllegalArgumentException("Invalid MATLAB function name: " + functionName + "\n" +
						"Function name must consist only of letters, numbers, and underscores");
			}
		}
	}

	@Override
	public String toString() {
		return "[" + this.getClass().getName() + " functionName=" + _function + "]";
	}

	@Override
	MatlabTypeSetter getSetter() {
		return new MatlabFunctionHandlerSetter(_function);
	}

	private static class MatlabFunctionHandlerSetter implements MatlabTypeSetter {
		private static final long serialVersionUID = 1909686398012439080L;
		private final String _function;

		public MatlabFunctionHandlerSetter(String function) {
			_function = function;
		}

		@Override
		public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			if (_function.startsWith("@")) {
				ops.eval(variableName + " = " + _function + ";");
			} else {
				ops.eval(variableName + " = @" + _function + ";");
			}
		}
	}

	static class MatlabFunctionHandleGetter implements MatlabTypeGetter<MatlabFunctionHandle> {
		private static final long serialVersionUID = 4448554689248088229L;
		private String _function;
		private boolean _retrieved = false;

		@Override
		public MatlabFunctionHandle retrieve() {
			if (_retrieved) {
				return new MatlabFunctionHandle(_function);
			} else {
				throw new IllegalStateException("MatlabFunctionHandle cannot be deserialized until the data has been " +
						"retrieved from MATLAB");
			}
		}

		@Override
		public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			_function = (String) ops.returningEval("func2str(" + variableName + ");", 1)[0];

			_retrieved = true;
		}
	}
}
