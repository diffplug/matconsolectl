/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Represents a variable in MATLAB. The representation is not associated with a given session of MATLAB. An instance of
 * this class with a given name does not mean that a variable with that name exists in any session of MATLAB.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabVariable extends MatlabType {
	private final String _name;

	/**
	 * Constructs a representation of a MATLAB variable with the name specified by {@code name}.
	 * 
	 * @param name 
	 * @throws IllegalArgumentException if {@code name} is not a valid MATLAB variable name
	 */
	public MatlabVariable(String name) {
		//Validate variable name
		if (name.isEmpty()) {
			throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
		}
		char[] nameChars = name.toCharArray();
		if (!Character.isLetter(nameChars[0])) {
			throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
		}
		for (char element : nameChars) {
			if (!(Character.isLetter(element) || Character.isDigit(element) || element == '_')) {
				throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
			}
		}
		_name = name;
	}

	/**
	 * The name of this variable.
	 * 
	 * @return 
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Returns a brief description of this variable. The exact details of this representation are unspecified and are
	 * subject to change.
	 * 
	 * @return 
	 */
	@Override
	public String toString() {
		return "[" + this.getClass().getName() + " name=" + _name + "]";
	}

	/**
	 * Returns {@code true} if and only if {@code obj} is a {@code MatlabVariable} and has the same name as this
	 * variable.
	 * 
	 * @param obj
	 * @return 
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equal = false;

		if (obj instanceof MatlabVariable) {
			MatlabVariable other = (MatlabVariable) obj;
			equal = other._name.equals(_name);
		}

		return equal;
	}

	/**
	 * Returns a hash code consistent with {@link #equals(java.lang.Object)}.
	 * 
	 * @return 
	 */
	@Override
	public int hashCode() {
		return _name.hashCode();
	}

	static class MatlabVariableGetter implements MatlabTypeGetter<MatlabVariable> {
		private static final long serialVersionUID = 7724337165919355824L;
		private String _name;
		private boolean _retrieved;

		@Override
		public MatlabVariable retrieve() {
			if (!_retrieved) {
				throw new IllegalStateException("variable not retrieved");
			}

			return new MatlabVariable(_name);
		}

		@Override
		public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			_name = variableName;
			_retrieved = true;
		}
	}

	@Override
	MatlabTypeSetter getSetter() {
		return new MatlabVariableSetter(_name);
	}

	private static class MatlabVariableSetter implements MatlabTypeSetter {
		private static final long serialVersionUID = -2208485477826441076L;
		private final String _name;

		private MatlabVariableSetter(String name) {
			_name = name;
		}

		@Override
		public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			ops.eval(variableName + " = " + _name + ";");
		}
	}
}
