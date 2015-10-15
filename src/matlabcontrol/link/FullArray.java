/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * 
 * @param <L> underlying linear array - single dimensional array type, ex. {@code byte[]}
 * @param <T> output array - primitive numeric array type, ex. {@code byte[][][]}
 *            (1 or more dimensions is acceptable, including for example {@code byte[]})
 */
class FullArray<L, T> extends BaseArray<L, T> {
	/**
	 * The lengths of each dimension of the array when represented as an array of type {@code T}.
	 */
	final int[] _dimensions;

	/**
	 * The total number of elements represented by this full array. This includes the zero elements represented by
	 * this array. This is equivalent to multiplying all values of {@link #_dimensions} together.
	 */
	private final int _numberOfElements;

	/**
	 * The primitive numeric type stored by the arrays.
	 */
	final Class<?> _baseComponentType;

	/**
	 * Internal linear array type.
	 */
	final Class<L> _linearArrayType;

	/**
	 * Output array type.
	 */
	final Class<T> _outputArrayType;

	/**
	 * The linear array of real values.
	 */
	final L _real;

	/**
	 * The linear array of imaginary values. Can be {@code null} if this number array is real.
	 */
	final L _imag;

	/**
	 * Caches if {@link #_imag} contains non-zero elements.
	 * <br><br>
	 * To avoid any form of inter-thread communication this value may in the most degenerate case be recomputed for each
	 * thread.
	 */
	private Boolean _hasImaginaryValues = null;

	/**
	 * Caches the hash code.
	 * <br><br>
	 * To avoid any form of inter-thread communication this value may in the most degenerate case be recomputed for each
	 * thread.
	 */
	private Integer _hashCode = null;

	/**
	 * Data from MATLAB. Provided as the linear arrays and dimensions.
	 * 
	 * @param linearArrayType
	 * @param realLinear
	 * @param imagLinear
	 * @param dimensions 
	 */
	@SuppressWarnings("unchecked")
	FullArray(Class<L> linearArrayType, L real, L imag, int[] dimensions) {
		//Dimensions of the array
		_dimensions = dimensions;
		_numberOfElements = ArrayUtils.getNumberOfElements(_dimensions);

		//The real and imaginary arrays should always be of type L, but validate it
		_real = linearArrayType.cast(real);
		_imag = linearArrayType.cast(imag);

		//Make class information at run time
		_baseComponentType = linearArrayType.getComponentType();
		_linearArrayType = linearArrayType;
		_outputArrayType = (Class<T>) ArrayUtils.getArrayClass(_baseComponentType, dimensions.length);
	}

	@SuppressWarnings("unchecked")
	FullArray(Class<L> linearArrayType, T real, T imag) {
		//Real array cannot be null
		if (real == null) {
			throw new NullPointerException("Real array may not be null");
		}

		//Make class information at run time
		_baseComponentType = linearArrayType.getComponentType();
		_linearArrayType = linearArrayType;
		_outputArrayType = (Class<T>) real.getClass();

		//Confirm real array is actually an array
		Class<?> realClass = real.getClass();
		if (!realClass.isArray()) {
			throw new IllegalArgumentException("Real array is not an array, type: " + realClass.getCanonicalName());
		}

		//Confirm the real array is of the supported type
		Class<?> realBaseComponentType = ArrayUtils.getBaseComponentType(realClass);
		if (!realBaseComponentType.equals(_baseComponentType)) {
			throw new IllegalArgumentException("Real array is not an array of the required class\n" +
					"Required base component type: " + _baseComponentType.getCanonicalName() + "\n" +
					"Provided base component type: " + realBaseComponentType.getCanonicalName());
		}

		//Confirm the imag array is of the same type as the real array
		if (imag != null && !imag.getClass().equals(realClass)) {
			throw new IllegalArgumentException("Imaginary array is not of the same class as the real array\n" +
					"Real array class: " + realClass.getCanonicalName() + "\n" +
					"Imaginary array class: " + imag.getClass().getCanonicalName());
		}

		//Determine dimensions
		_dimensions = new int[ArrayUtils.getNumberOfDimensions(_outputArrayType)];
		int[] realDimensions = ArrayUtils.computeBoundingDimensions(real);
		for (int i = 0; i < realDimensions.length; i++) {
			_dimensions[i] = Math.max(_dimensions[i], realDimensions[i]);
		}
		if (imag != null) {
			int[] imagDimensions = ArrayUtils.computeBoundingDimensions(imag);
			for (int i = 0; i < imagDimensions.length; i++) {
				_dimensions[i] = Math.max(_dimensions[i], imagDimensions[i]);
			}
		}
		_numberOfElements = ArrayUtils.getNumberOfElements(_dimensions);

		//Linearize arrays
		_real = _linearArrayType.cast(ArrayLinearizer.linearize(real, _dimensions));
		if (imag != null) {
			_imag = _linearArrayType.cast(ArrayLinearizer.linearize(imag, _dimensions));
		} else {
			_imag = null;
			_hasImaginaryValues = false;
		}
	}

	@Override
	int getNumberOfElements() {
		return _numberOfElements;
	}

	@Override
	int getLengthOfDimension(int dimension) {
		if (dimension >= _dimensions.length || dimension < 0) {
			throw new IllegalArgumentException(dimension + " is not a dimension of this array. This array has " +
					getNumberOfDimensions() + " dimensions");
		}

		return _dimensions[dimension];
	}

	@Override
	int getNumberOfDimensions() {
		return _dimensions.length;
	}

	@Override
	boolean isReal() {
		if (_hasImaginaryValues == null) {
			_hasImaginaryValues = ArrayUtils.containsNonDefaultValue(_imag);
		}

		return !_hasImaginaryValues;
	}

	@Override
	T toRealArray() {
		return _outputArrayType.cast(ArrayMultidimensionalizer.multidimensionalize(_real, _dimensions));
	}

	@Override
	T toImaginaryArray() {
		T array;
		if (isReal()) {
			array = _outputArrayType.cast(Array.newInstance(_baseComponentType, _dimensions));
		} else {
			array = _outputArrayType.cast(ArrayMultidimensionalizer.multidimensionalize(_imag, _dimensions));
		}

		return array;
	}

	@Override
	boolean isSparse() {
		return false;
	}

	int getLinearIndex(int row, int column) {
		return ArrayUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column);
	}

	int getLinearIndex(int row, int column, int page) {
		return ArrayUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column, page);
	}

	int getLinearIndex(int row, int column, int[] pages) {
		return ArrayUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column, pages);
	}

	@Override
	public boolean equals(Object obj) {
		boolean equal = false;

		//Same object
		if (this == obj) {
			equal = true;
		}
		//Same class
		else if (obj != null && this.getClass().equals(obj.getClass())) {
			FullArray<?, ?> other = (FullArray<?, ?>) obj;

			//If the two instances are equal their hashcodes must be equal (but not the converse)
			if (this.hashCode() == other.hashCode()) {
				//Check equality of the elements of the arrays in expected increasing order of computational complexity

				//Same base components
				if (_baseComponentType.equals(other._baseComponentType)) {
					//Both real values, or both complex values
					if ((this.isReal() && other.isReal()) || (!this.isReal() && !other.isReal())) {
						//Same dimensions
						if (Arrays.equals(_dimensions, other._dimensions)) {
							//Finally, compare the inner arrays
							equal = ArrayUtils.equals(_real, other._real) &&
									ArrayUtils.equals(_imag, other._imag);
						}
					}
				}
			}
		}

		return equal;
	}

	@Override
	public int hashCode() {
		if (_hashCode == null) {
			int hashCode = 7;

			hashCode = 97 * hashCode + _baseComponentType.hashCode();
			hashCode = 97 * hashCode + ArrayUtils.hashCode(_real);
			hashCode = 97 * hashCode + ArrayUtils.hashCode(_imag);
			hashCode = 97 * hashCode + Arrays.hashCode(_dimensions);

			_hashCode = hashCode;
		}

		return _hashCode;
	}
}
