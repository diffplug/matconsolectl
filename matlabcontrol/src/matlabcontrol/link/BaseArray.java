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
 * 
 * @param <L> underlying linear array - single dimensional array type, ex. {@code byte[]}
 * @param <T> output array - primitive numeric array type, ex. {@code byte[][][]}
 *            (1 or more dimensions is acceptable, including for example {@code byte[]})
 */
abstract class BaseArray<L, T> {
	/**
	 * Returns {@code true} if the array has no imaginary values, {@code false} otherwise. Equivalent to the MATLAB
	 * {@code isreal} function.
	 * 
	 * @return 
	 */
	abstract boolean isReal();

	/**
	 * The number of elements in the array. Zero elements are counted. The real and imaginary components of a number are
	 * together considered one element. This is equivalent to MATLAB's {@code numel} function.
	 * 
	 * @return number of elements
	 */
	abstract int getNumberOfElements();

	/**
	 * Returns the length of the dimension specified by {@code dimension}. Dimensions use 0-based indexing. So the
	 * first dimension, which is dimension 0, is the row length. The second dimension is the column length. The third
	 * dimension and beyond are pages.
	 * 
	 * @param dimension
	 * @return length of {@code dimension}
	 * @throws IllegalArgumentException if {@code dimension} is not a dimension of the array
	 */
	abstract int getLengthOfDimension(int dimension);

	/**
	 * Returns the number of dimensions of the array.
	 * 
	 * @return number of dimensions
	 */
	abstract int getNumberOfDimensions();

	/**
	 * Returns an array that holds the real values from the MATLAB array. Each call returns a new copy which may be used
	 * in any manner; modifications to it will have no effect on this instance.
	 * 
	 * @return real array
	 */
	abstract T toRealArray();

	/**
	 * Returns an array that holds the imaginary values from the MATLAB array. Each call returns a new copy which may be
	 * used in any manner; modifications to it will have no effect on this instance. If this array is real then the
	 * returned array will be have the default value as all of its base elements.
	 * 
	 * @return imaginary array
	 */
	abstract T toImaginaryArray();

	/**
	 * If this array has a sparse representation.
	 * 
	 * @return 
	 */
	abstract boolean isSparse();
}
