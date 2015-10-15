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
 * @param <T> {@code long} array type, ex. {@code long[]}, {@code long[][]}, {@code long[][][]}, ...
 */
public class MatlabInt64Array<T> extends MatlabNumberArray<long[], T> {
	MatlabInt64Array(long[] real, long[] imag, int[] lengths) {
		super(long[].class, real, imag, lengths);
	}

	public static <T> MatlabInt64Array<T> getInstance(T real, T imaginary) {
		return new MatlabInt64Array<T>(real, imaginary);
	}

	private MatlabInt64Array(T real, T imaginary) {
		super(long[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt64 getElementAtLinearIndex(int index) {
		return new MatlabInt64(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt64 getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabInt64(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(long[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(long[] other) {
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
	boolean containsNonZero(long[] array) {
		boolean contained = false;

		for (long val : array) {
			if (val != 0L) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
