/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * MATLAB {@code int16} with real and imaginary components.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabInt16 extends MatlabNumber<Short> {
	private static final Short DEFAULT = 0;

	public MatlabInt16(short real, short imag) {
		super(DEFAULT, real, imag);
	}

	/**
	 * Returns the real value.
	 * 
	 * @return 
	 */
	public short toReal() {
		return _real;
	}

	/**
	 * Returns the imaginary value.
	 * 
	 * @return 
	 */
	public short toImaginary() {
		return _imag;
	}
}
