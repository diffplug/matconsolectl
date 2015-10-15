/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Corresponds to a MATLAB number with real and imaginary components.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabNumber<T extends Number> extends MatlabType {
	final T _real;
	final T _imag;

	/**
	 * The default value for {@code T}.
	 */
	private final T _default;

	MatlabNumber(T defaultValue, T real, T imag) {
		_default = defaultValue;

		_real = real;
		_imag = imag;
	}

	/**
	 * Returns {@code true} if the imaginary value is equivalent to the default value for the numeric type,
	 * {@code false} otherwise. Equivalent to the MATLAB {@code isreal} function.
	 * 
	 * @return 
	 */
	public boolean isReal() {
		return _default.equals(_imag);
	}

	/**
	 * Returns a {@code String} representation of the number. If the number is real the String representation is
	 * <i>real</i>. If an imaginary component is present, the format is <code><i>real</i> + <i>imag</i>i</code> if the
	 * imaginary component is non-negative and <code><i>real</i> - <i>imag</i>i</code> if the imaginary component is
	 * negative.
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		String fullStr;
		String realStr = _real.toString();

		if (this.isReal()) {
			fullStr = realStr;
		} else {
			String imagStr = _imag.toString();
			if (imagStr.startsWith("-")) {
				fullStr = realStr + " - " + imagStr.substring(1, imagStr.length()) + "i";
			} else {
				fullStr = realStr + " + " + imagStr + "i";
			}
		}

		return fullStr;
	}

	/**
	 * Returns {@code true} if and only if the other object is of the same class and its component real and imaginary
	 * parts are both equal. The definition of equality for the component parts is defined by the {@code equals(...)}
	 * method of the boxed numeric class of its components.
	 * 
	 * @param obj
	 * @return 
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equal = false;

		if (obj != null && this.getClass().equals(obj.getClass())) {
			MatlabNumber<?> other = (MatlabNumber<?>) obj;
			equal = _real.equals(other._real) && _imag.equals(other._imag);
		}

		return equal;
	}

	/**
	 * Returns a hash code compatible with {@code equals(...)}.
	 * 
	 * @return 
	 */
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 79 * hash + _real.hashCode();
		hash = 79 * hash + _imag.hashCode();

		return hash;
	}

	@Override
	MatlabTypeSetter getSetter() {
		return new MatlabNumberSetter(_real, _imag);
	}

	//This is very non-object oriented to have this here instead of each subclass, but it saves a lot of boilerplate
	private static class MatlabNumberSetter implements MatlabTypeSetter {
		private static final long serialVersionUID = -8427476815084218279L;
		private final Number _real;
		private final Number _imag;

		private MatlabNumberSetter(Number real, Number imag) {
			_real = real;
			_imag = imag;
		}

		@Override
		public void setInMatlab(MatlabOperations ops, String name) throws MatlabInvocationException {
			//Avoids limitation that MATLAB cannot multiply an integer stored in a variable by i

			if (_real instanceof Byte) {
				ops.eval(name + "=int8(" + _real.byteValue() + "+" + _imag.byteValue() + "i);");
			} else if (_real instanceof Short) {
				ops.eval(name + "=int16(" + _real.shortValue() + "+" + _imag.shortValue() + "i);");
			} else if (_real instanceof Integer) {
				ops.eval(name + "=int32(" + _real.intValue() + "+" + _imag.intValue() + "i);");
			} else if (_real instanceof Long) {
				ops.eval(name + "=int64(" + _real.longValue() + "+" + _imag.longValue() + "i);");
			}

			// Set value through an array to avoid values being converted to MATLAB double

			else if (_real instanceof Float) {
				ops.setVariable(name, new float[]{_real.floatValue(), _imag.floatValue()});
				ops.eval(name + "=" + name + "(1)+" + name + "(2)*i;");
			} else if (_real instanceof Double) {
				ops.setVariable(name, new double[]{_real.doubleValue(), _imag.doubleValue()});
				ops.eval(name + "=" + name + "(1)+" + name + "(2)*i;");
			}
		}
	}

	static class MatlabNumberGetter implements MatlabTypeGetter<MatlabNumber<?>> {
		private static final long serialVersionUID = 1339080882185682568L;
		private Object _real;
		private Object _imag;
		private boolean _retrieved;

		@Override
		public MatlabNumber<?> retrieve() {
			if (!_retrieved) {
				throw new IllegalStateException("complex number has not been retrieved");
			}

			MatlabNumber<?> num;
			if (_real.getClass().equals(Byte.class)) {
				num = new MatlabInt8((Byte) _real, (Byte) _imag);
			} else if (_real.getClass().equals(Short.class)) {
				num = new MatlabInt16((Short) _real, (Short) _imag);
			} else if (_real.getClass().equals(Integer.class)) {
				num = new MatlabInt32((Integer) _real, (Integer) _imag);
			} else if (_real.getClass().equals(Long.class)) {
				num = new MatlabInt64((Long) _real, (Long) _imag);
			} else if (_real.getClass().equals(Float.class)) {
				num = new MatlabSingle((Float) _real, (Float) _imag);
			} else if (_real.getClass().equals(Double.class)) {
				num = new MatlabDouble((Double) _real, (Double) _imag);
			} else {
				throw new IllegalStateException("unsupported numeric type: " + _real.getClass().getCanonicalName());
			}

			return num;
		}

		@Override
		public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			String receiverName = (String) ops.returningEval("genvarname('complex_receiver_', who);", 1)[0];
			try {
				ops.setVariable(receiverName, this);
				ops.eval(receiverName + ".setReal(real(" + variableName + "));");
				ops.eval(receiverName + ".setImaginary(imag(" + variableName + "));");
			} finally {
				ops.eval("clear " + receiverName);
			}

			_retrieved = true;
		}

		/**
		 * Sets the real value. Only meant to be called from MATLAB. In order for MATLAB to pass in the number, this
		 * method must have {@code Object} as its parameter, not {@code Number}.
		 * 
		 * @param real 
		 */
		public void setReal(Object real) {
			_real = real;
		}

		/**
		 * Sets the imaginary value. Only meant to be called from MATLAB. In order for MATLAB to pass in the number,
		 * this method must have {@code Object} as its parameter, not {@code Number}.
		 * 
		 * @param imag 
		 */
		public void setImaginary(Object imag) {
			_imag = imag;
		}
	}
}
