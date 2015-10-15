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
 * @param <T> {@code byte} array type, ex. {@code byte[]}, {@code byte[][]}, {@code byte[][][]}, ...
 */
public class MatlabInt8Array<T> extends MatlabNumberArray<byte[], T> {
	MatlabInt8Array(byte[] real, byte[] imag, int[] lengths) {
		super(byte[].class, real, imag, lengths);
	}

	public static <T> MatlabInt8Array<T> getInstance(T real, T imaginary) {
		return new MatlabInt8Array<T>(real, imaginary);
	}

	private MatlabInt8Array(T real, T imaginary) {
		super(byte[].class, real, imaginary);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt8 getElementAtLinearIndex(int index) {
		return new MatlabInt8(_real[index], (_imag == null ? 0 : _imag[index]));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalArgumentException {@inheritDoc}
	 * @throws ArrayIndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public MatlabInt8 getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = getLinearIndex(row, column, pages);

		return new MatlabInt8(_real[linearIndex], (_imag == null ? 0 : _imag[linearIndex]));
	}

	@Override
	boolean equalsRealArray(byte[] other) {
		return Arrays.equals(_real, other);
	}

	@Override
	boolean equalsImaginaryArray(byte[] other) {
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
	boolean containsNonZero(byte[] array) {
		boolean contained = false;

		for (byte val : array) {
			if (val != 0) {
				contained = true;
				break;
			}
		}

		return contained;
	}
}
