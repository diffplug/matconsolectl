/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * The base class for all MATLAB matrices.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabMatrix<L, T> {
	abstract BaseArray<L, T> getBaseArray();

	/**
	 * The number of elements in the array. The real and imaginary components of a number are together considered one
	 * element. This is equivalent to MATLAB's {@code numel} function.
	 * 
	 * @return number of elements
	 */
	public int getNumberOfElements() {
		return getBaseArray().getNumberOfDimensions();
	}

	/**
	 * Returns the length of the dimension specified by {@code dimension}. Dimensions use 0-based indexing. So the
	 * first dimension, which is dimension 0, is the row length. The second dimension is the column length. The third
	 * dimension and beyond are pages.
	 * 
	 * @param dimension
	 * @return length of {@code dimension}
	 * @throws IllegalArgumentException if {@code dimension} is not a dimension of the array
	 */
	public int getLengthOfDimension(int dimension) {
		return getBaseArray().getLengthOfDimension(dimension);
	}

	/**
	 * Returns the number of dimensions of the array.
	 * 
	 * @return number of dimensions
	 */
	public int getNumberOfDimensions() {
		return getBaseArray().getNumberOfDimensions();
	}
}
