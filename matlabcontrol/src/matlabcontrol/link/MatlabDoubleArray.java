/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.util.Arrays;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * @param <T> {@code double} array type, ex. {@code double[]}, {@code double[][]}, {@code double[][][]}, ...
 */
public class MatlabDoubleArray<T> extends MatlabNumberArray<double[], T> {
	MatlabDoubleArray(double[] real, double[] imag, int[] lengths) {
		super(double[].class, real, imag, lengths);
	}

	public static <T> MatlabDoubleArray<T> getInstance(T real, T imaginary) {
		return new MatlabDoubleArray<T>(real, imaginary);
	}

	private MatlabDoubleArray(T real, T imaginary) {
		super(double[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabDouble getElementAtLinearIndex(int index) {
		return new MatlabDouble(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabDouble getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabDouble(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(double[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(double[] other) {
		return Arrays.equals(_imag, other);
	}

	@Override
	int hashReal() {
		return Arrays.hashCode(_real);
	}

	@Override
	int hashImaginary() {
		return Arrays.hashCode(_imag);
	}

	@Override
	boolean containsNonZero(double[] array) {
		boolean contained = false;

		for (double val : array) {
			if (val != 0.0d) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
