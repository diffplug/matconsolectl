/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * MATLAB {@code int8} with real and imaginary components.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabInt8 extends MatlabNumber<Byte> {
	private static final Byte DEFAULT = 0;

	public MatlabInt8(byte real, byte imag) {
		super(DEFAULT, real, imag);
	}

	/**
	 * Returns the real value.
	 * 
	 * @return 
	 */
	public byte toReal() {
		return _real;
	}

	/**
	 * Returns the imaginary value.
	 * 
	 * @return 
	 */
	public byte toImaginary() {
		return _imag;
	}
}
