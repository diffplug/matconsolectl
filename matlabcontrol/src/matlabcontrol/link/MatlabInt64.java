/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * MATLAB {@code int64} with real and imaginary components.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabInt64 extends MatlabNumber<Long> {
	private static final Long DEFAULT = 0L;

	public MatlabInt64(long real, long imag) {
		super(DEFAULT, real, imag);
	}

	/**
	 * Returns the real value.
	 * 
	 * @return 
	 */
	public long toReal() {
		return _real;
	}

	/**
	 * Returns the imaginary value.
	 * 
	 * @return 
	 */
	public long toImaginary() {
		return _imag;
	}
}
