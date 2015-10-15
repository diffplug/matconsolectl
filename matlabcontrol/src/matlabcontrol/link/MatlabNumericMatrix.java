/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabNumericMatrix<L, T> extends MatlabMatrix<L, T> {
	/**
	 * Returns {@code true} if the array has no imaginary values, {@code false} otherwise. Equivalent to the MATLAB
	 * {@code isreal} function.
	 * 
	 * @return 
	 */
	public boolean isReal() {
		return getBaseArray().isReal();
	}

	/**
	 * Returns an array that holds the real values from the MATLAB array. Each call returns a new copy which may be used
	 * in any manner; modifications to it will have no effect on this instance.
	 * 
	 * @return real array
	 */
	public T toRealArray() {
		return getBaseArray().toRealArray();
	}

	/**
	 * Returns an array that holds the imaginary values from the MATLAB array. Each call returns a new copy which may be
	 * used in any manner; modifications to it will have no effect on this instance. If this array is real then the
	 * returned array will be have {@code 0} as all of its base elements.
	 * 
	 * @return imaginary array
	 */
	public T toImaginaryArray() {
		return getBaseArray().toImaginaryArray();
	}

	/**
	 * Gets the element at {@code index} treating this array as a MATLAB column vector. This is equivalent to indexing
	 * into a MATLAB array with just one subscript.
	 * 
	 * @param index
	 * @return element at {@code index}
	 * @throws ArrayIndexOutOfBoundsException if {@code index} is out of bounds
	 */
	public abstract MatlabNumber<?> getElementAtLinearIndex(int index);

	public abstract MatlabNumber<?> getElementAtIndices(int row, int column);

	public abstract MatlabNumber<?> getElementAtIndices(int row, int column, int page);

	/**
	 * Gets the element at the specified {@code row}, {@code column}, and {@code pages}.
	 * 
	 * @param row
	 * @param column
	 * @param pages
	 * @return element at {@code row}, {@code column}, and {@code pages}
	 * @throws IllegalArgumentException if number of indices does not equal this array's number of dimensions
	 * @throws ArrayIndexOutOfBoundsException if the indices are out of bound
	 */
	public abstract MatlabNumber<?> getElementAtIndices(int row, int column, int... pages);
}
