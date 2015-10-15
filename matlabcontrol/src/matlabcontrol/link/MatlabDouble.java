/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * MATLAB {@code double} with real and imaginary components.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabDouble extends MatlabNumber<Double> {
	private static final Double DEFAULT = 0.0d;

	public MatlabDouble(double real, double imag) {
		super(DEFAULT, real, imag);
	}

	/**
	 * Returns the real value.
	 * 
	 * @return 
	 */
	public double toReal() {
		return _real;
	}

	/**
	 * Returns the imaginary value.
	 * 
	 * @return 
	 */
	public double toImaginary() {
		return _imag;
	}
}
