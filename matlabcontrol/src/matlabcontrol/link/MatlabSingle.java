/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * MATLAB {@code single} with real and imaginary components.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabSingle extends MatlabNumber<Float> {
	private static final Float DEFAULT = 0.0f;

	public MatlabSingle(float real, float imag) {
		super(DEFAULT, real, imag);
	}

	/**
	 * Returns the real value.
	 * 
	 * @return 
	 */
	public float toReal() {
		return _real;
	}

	/**
	 * Returns the imaginary value.
	 * 
	 * @return 
	 */
	public float toImaginary() {
		return _imag;
	}
}
