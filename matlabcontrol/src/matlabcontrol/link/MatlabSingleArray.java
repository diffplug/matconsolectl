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
 * @param <T> {@code float} array type, ex. {@code float[]}, {@code float[][]}, {@code float[][][]}, ...
 */
public class MatlabSingleArray<T> extends MatlabNumberArray<float[], T> {
	MatlabSingleArray(float[] real, float[] imag, int[] lengths) {
		super(float[].class, real, imag, lengths);
	}

	public static <T> MatlabSingleArray<T> getInstance(T real, T imaginary) {
		return new MatlabSingleArray<T>(real, imaginary);
	}

	private MatlabSingleArray(T real, T imaginary) {
		super(float[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabSingle getElementAtLinearIndex(int index) {
		return new MatlabSingle(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabSingle getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabSingle(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(float[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(float[] other) {
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
	boolean containsNonZero(float[] array) {
		boolean contained = false;

		for (float val : array) {
			if (val != 0.0f) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
