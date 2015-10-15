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
 * @param <T> {@code short} array type, ex. {@code short[]}, {@code short[][]}, {@code short[][][]}, ...
 */
public class MatlabInt16Array<T> extends MatlabNumberArray<short[], T> {
	MatlabInt16Array(short[] real, short[] imag, int[] lengths) {
		super(short[].class, real, imag, lengths);
	}

	public static <T> MatlabInt16Array<T> getInstance(T real, T imaginary) {
		return new MatlabInt16Array<T>(real, imaginary);
	}

	private MatlabInt16Array(T real, T imaginary) {
		super(short[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt16 getElementAtLinearIndex(int index) {
		return new MatlabInt16(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt16 getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabInt16(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(short[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(short[] other) {
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
	boolean containsNonZero(short[] array) {
		boolean contained = false;

		for (short val : array) {
			if (val != 0) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
