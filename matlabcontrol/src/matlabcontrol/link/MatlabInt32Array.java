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
 * @param <M> {@code int} array type, ex. {@code int[]}, {@code int[][]}, {@code int[][][]}, ...
 */
public class MatlabInt32Array<T> extends MatlabNumberArray<int[], T> {
	MatlabInt32Array(int[] real, int[] imag, int[] lengths) {
		super(int[].class, real, imag, lengths);
	}

	public static <T> MatlabInt32Array<T> getInstance(T real, T imaginary) {
		return new MatlabInt32Array<T>(real, imaginary);
	}

	private MatlabInt32Array(T real, T imaginary) {
		super(int[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt32 getElementAtLinearIndex(int index) {
		return new MatlabInt32(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt32 getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabInt32(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(int[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(int[] other) {
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
	boolean containsNonZero(int[] array) {
		boolean contained = false;

		for (int val : array) {
			if (val != 0) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
