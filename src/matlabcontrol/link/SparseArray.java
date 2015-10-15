/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MATLAB sparse matrices are always two dimensional. This class models a two dimensional sparse array.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * 
 * @param <L> underlying array of values - single dimensional array type, ex. {@code byte[]}
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SparseArray<L> extends BaseArray<L, L[]> {
	/**
	 * The values in this array are the linear indices in the sparse array that have values. The index of an element
	 * in this array maps to the index in {@link #_realValues} and {@link #_imagValues} that contain the value at the
	 * linear index represented by the element as well as to the corresponding {@link #_rowIndices} and
	 * {@link #_colIndices}. While the row and column indices can be calculated from these linear indices, these will
	 * be needed as arrays in order to call MATLAB's {@code sparse} function.
	 */
	private final int[] _linearIndices;

	/**
	 * The row indices for the values represented by this sparse array.
	 * <br><br>
	 * Corresponds to the {@code i} parameter of MATLAB's {@code sparse} function.
	 */
	private final int[] _rowIndices;

	/**
	 * The column indices for the values represented by this sparse array.
	 * <br><br>
	 * Corresponds to the {@code j} parameter of MATLAB's {@code sparse} function.
	 */
	private final int[] _colIndices;

	/**
	 * A single dimension array of real values.
	 * <br><br>
	 * Corresponds to the real value portion of the {@code s} parameter to MATLAB's {@code sparse} function.
	 */
	final L _realValues;

	/**
	 * A single dimension array of imaginary values. Can be {@code null} if this sparse array is real.
	 * <br><br>
	 * Corresponds to the imaginary value portion of the {@code s} parameter to MATLAB's {@code sparse} function.
	 */
	final L _imagValues;

	/**
	 * Caches the hash code.
	 * <br><br>
	 * To avoid any form of inter-thread communication this value may in the most degenerate case be recomputed for each
	 * thread.
	 */
	private Integer _hashCode = null;

	/**
	 * The primitive numeric type stored by the arrays.
	 */
	private final Class<?> _baseComponentType;

	/**
	 * Output array type.
	 */
	private final Class<L[]> _outputArrayType;

	private final int _numRows;

	private final int _numCols;

	/**
	 * Data from MATLAB. Provided as the indices, linear arrays of values, and dimensions. The indices are expected to
	 * be sorted in increasing value and the real and imaginary values are to correspond to these indices.
	 * 
	 * @param linearArrayType
	 * @param linearIndices need to be sorted in ascending value
	 * @param realLinear
	 * @param imagLinear
	 * @param numRows
	 * @param numCols
	 */
	SparseArray(Class<L> linearArrayType, int[] linearIndices, int[] rowIndices, int[] colIndices, L real, L imag,
			int numRows, int numCols) {
		//Dimensions
		_numRows = numRows;
		_numCols = numCols;

		//Make class information at run time
		_baseComponentType = linearArrayType.getComponentType();
		_outputArrayType = (Class<L[]>) ArrayUtils.getArrayClass(_baseComponentType, 2);

		//Indices
		_linearIndices = linearIndices;
		_rowIndices = rowIndices;
		_colIndices = colIndices;

		//The real and imaginary arrays should always be of type L, but validate it
		_realValues = linearArrayType.cast(real);
		_imagValues = linearArrayType.cast(imag);
	}

	/**
	 * Data provided by a user; this data needs to be validated and processed.
	 * 
	 * @param arrayType
	 * @param rowIndices
	 * @param colIndices
	 * @param realValues
	 * @param imagValues
	 * @param numRows
	 * @param numCols 
	 */
	SparseArray(Class<L> linearArrayType, int[] rowIndices, int[] colIndices, L realValues, L imagValues,
			int numRows, int numCols) {
		validateUserSuppliedParameters(linearArrayType, rowIndices, colIndices, realValues, imagValues);

		//Make class information at run time
		_baseComponentType = linearArrayType.getComponentType();
		_outputArrayType = (Class<L[]>) ArrayUtils.getArrayClass(_baseComponentType, 2);

		//Construct a sparse mapping, sort by keys, and then set these values in to the indice and value arrays
		Map<SparseKey, SparseValue> sparseMap = createSparseMap(linearArrayType, rowIndices, colIndices, realValues,
				imagValues, numRows, numCols);
		_numRows = numRows;
		_numCols = numCols;
		ArrayList<SparseKey> keys = new ArrayList<SparseKey>(sparseMap.keySet());
		Collections.sort(keys);
		_rowIndices = new int[keys.size()];
		_colIndices = new int[keys.size()];
		_linearIndices = new int[keys.size()];
		_realValues = linearArrayType.cast(Array.newInstance(_baseComponentType, keys.size()));
		_imagValues = imagValues == null ? null : linearArrayType.cast(Array.newInstance(_baseComponentType, keys.size()));
		for (int i = 0; i < keys.size(); i++) {
			SparseKey key = keys.get(i);
			_rowIndices[i] = key.row;
			_colIndices[i] = key.col;
			_linearIndices[i] = key.linearIndex;

			SparseValue value = sparseMap.get(key);
			setSparseValue(value, _realValues, _imagValues, i);
		}
	}

	@Override
	boolean isReal() {
		//For a sparse array, if there are no imaginary values then it is real. There is no need to check if all
		//entries of the imaginary array are zero as zero entries are not stored for sparse arrays.

		return (_imagValues == null);
	}

	@Override
	L[] toRealArray() {
		return sparseTo2DArray(_outputArrayType, _rowIndices, _colIndices, _realValues, _numRows, _numCols);
	}

	@Override
	L[] toImaginaryArray() {
		return sparseTo2DArray(_outputArrayType, _rowIndices, _colIndices, _imagValues, _numRows, _numCols);
	}

	private static <L> L[] sparseTo2DArray(Class<L[]> array2DType, int[] rowIndices, int[] colIndices, L values,
			int numRows, int numCols) {
		L[] array2D = (L[]) Array.newInstance(array2DType, numRows, numCols);
		if (values != null) {
			SparseArrayFillOperation fillOperation = SPARSE_FILL_OPERATIONS.get(array2DType);
			fillOperation.fill(array2D, values, rowIndices, colIndices);
		}

		return array2D;
	}

	private static final Map<Class<?>, SparseArrayFillOperation<?>> SPARSE_FILL_OPERATIONS;

	static {
		Map<Class<?>, SparseArrayFillOperation<?>> map = new HashMap<Class<?>, SparseArrayFillOperation<?>>();

		map.put(byte[][].class, new ByteArrayFillOperation());
		map.put(short[][].class, new ShortArrayFillOperation());
		map.put(int[][].class, new IntArrayFillOperation());
		map.put(long[][].class, new LongArrayFillOperation());
		map.put(float[][].class, new FloatArrayFillOperation());
		map.put(double[][].class, new DoubleArrayFillOperation());
		map.put(boolean[][].class, new BooleanArrayFillOperation());
		map.put(char[][].class, new CharArrayFillOperation());

		SPARSE_FILL_OPERATIONS = Collections.unmodifiableMap(map);
	}

	@Override
	int getNumberOfElements() {
		return _numCols * _numRows;
	}

	@Override
	int getLengthOfDimension(int dimension) {
		int length;
		if (dimension == 0) {
			length = _numRows;
		} else if (dimension == 1) {
			length = _numCols;
		} else {
			throw new IllegalArgumentException(dimension + " is not a dimension of this array. This array has 2 " +
					"dimensions");
		}

		return length;
	}

	@Override
	int getNumberOfDimensions() {
		return 2;
	}

	private static interface SparseArrayFillOperation<T> {
		public void fill(T[] dst, T src, int[] rowIndices, int[] colIndices);
	}

	private static class ByteArrayFillOperation implements SparseArrayFillOperation<byte[]> {
		@Override
		public void fill(byte[][] dst, byte[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class ShortArrayFillOperation implements SparseArrayFillOperation<short[]> {
		@Override
		public void fill(short[][] dst, short[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class IntArrayFillOperation implements SparseArrayFillOperation<int[]> {
		@Override
		public void fill(int[][] dst, int[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class LongArrayFillOperation implements SparseArrayFillOperation<long[]> {
		@Override
		public void fill(long[][] dst, long[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class FloatArrayFillOperation implements SparseArrayFillOperation<float[]> {
		@Override
		public void fill(float[][] dst, float[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class DoubleArrayFillOperation implements SparseArrayFillOperation<double[]> {
		@Override
		public void fill(double[][] dst, double[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class CharArrayFillOperation implements SparseArrayFillOperation<char[]> {
		@Override
		public void fill(char[][] dst, char[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	private static class BooleanArrayFillOperation implements SparseArrayFillOperation<boolean[]> {
		@Override
		public void fill(boolean[][] dst, boolean[] src, int[] rowIndices, int[] colIndices) {
			for (int i = 0; i < src.length; i++) {
				dst[rowIndices[i]][colIndices[i]] = src[i];
			}
		}
	}

	@Override
	boolean isSparse() {
		return true;
	}

	/**
	 * Converts from a linear index for the array to the corresponding index in {@link #_real} and {@link #_imag}. If
	 * no corresponding index was found then the value returned will be negative.
	 * 
	 * @param linearIndex
	 * @return sparse index
	 */
	int getSparseIndexForLinearIndex(int linearIndex) {
		return Arrays.binarySearch(_linearIndices, linearIndex);
	}

	/**
	 * Converts from row and column indices for the array to the corresponding index in {@link #_real} and
	 * {@link #_imag}. If no corresponding index was found then the value returned will be negative.
	 * 
	 * @param row
	 * @param column
	 * @return sparse index
	 */
	int getSparseIndexForIndices(int row, int column) {
		int linearIndex = ArrayUtils.checkedMultidimensionalIndicesToLinearIndex(_numRows, _numCols, row, column);

		return Arrays.binarySearch(_linearIndices, linearIndex);
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
			SparseArray<?> other = (SparseArray<?>) obj;

			//If the two instances are equal their hashcodes must be equal (but not the converse)
			if (this.hashCode() == other.hashCode()) {
				//Check equality of the elements of the arrays in expected increasing order of computational complexity

				//Same base components
				if (_baseComponentType.equals(other._baseComponentType)) {
					//Both real values, or both complex values
					if ((this.isReal() && other.isReal()) || (!this.isReal() && !other.isReal())) {
						//Same dimensions
						if (_numRows == other._numRows && _numCols == other._numCols) {
							//Same indices
							if (Arrays.equals(_linearIndices, other._linearIndices)) {
								//Finally, compare the inner arrays
								equal = ArrayUtils.equals(_realValues, other._realValues) &&
										ArrayUtils.equals(_imagValues, other._imagValues);
							}
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
			hashCode = 97 * hashCode + Arrays.hashCode(_linearIndices);
			hashCode = 97 * hashCode + ArrayUtils.hashCode(_realValues);
			hashCode = 97 * hashCode + ArrayUtils.hashCode(_imagValues);
			hashCode = 97 * hashCode + _numRows;
			hashCode = 97 * hashCode + _numCols;

			_hashCode = hashCode;
		}

		return _hashCode;
	}

	/******************************************************************************************************************\
	|*                      Helper methods for creating a sparse array from user supplied arguments                   *|
	\******************************************************************************************************************/

	private static Map<SparseKey, SparseValue> createSparseMap(Class<?> arrayType, int[] rowIndices, int[] colIndices,
			Object realValues, Object imagValues, int numRows, int numCols) {
		HashMap<SparseKey, SparseValue> sparseMap = new HashMap<SparseKey, SparseValue>();
		for (int i = 0; i < rowIndices.length; i++) {
			//Validate the rows and col indices are within specified bounds
			int row = rowIndices[i];
			int col = colIndices[i];
			if (row >= numRows && col >= numCols) {
				throw new ArrayIndexOutOfBoundsException("row or column index is out of bounds\n" +
						"row: " + row + "\n" +
						"column: " + col + "\n" +
						"numRows: " + numRows + "\n" +
						"numColumns: " + numCols);
			}

			//Create a key based on the index in to the array
			SparseKey key = new SparseKey(row, col, numRows);

			//Determine the value of the entry, adding it to any value that already exists for this key
			SparseValue value = getSparseValue(realValues, imagValues, i);
			SparseValue existingValue = sparseMap.get(key);
			if (existingValue != null) {
				value = addSparseValue(value, existingValue);

				//If the resulting value is the default value, then remove the entry from the map
				if (value.isDefaultValue()) {
					sparseMap.remove(key);
				}
			}

			//If the value is not the default value, add the value to the map
			if (!value.isDefaultValue()) {
				sparseMap.put(key, value);
			}
		}

		return sparseMap;
	}

	private static void validateUserSuppliedParameters(Class<?> linearArrayType, int[] rowIndices, int[] colIndices,
			Object realValues, Object imagValues) {
		//Enforce required parameters not be null
		if (rowIndices == null) {
			throw new NullPointerException("rowIndices may not be null");
		}
		if (colIndices == null) {
			throw new NullPointerException("colIndices may not be null");
		}
		if (realValues == null) {
			throw new NullPointerException("realValues may not be null");
		}

		//Validate lengths of row and col indices
		if (rowIndices.length != colIndices.length) {
			throw new IllegalArgumentException("rowIndices and colIndices have differing lengths\n" +
					"rowIndices length: " + rowIndices.length + "\n" +
					"colIndices length: " + colIndices.length);
		}
		int realLength = Array.getLength(realValues);
		if (rowIndices.length != realLength) {
			throw new IllegalArgumentException("indices have differing lengths from the value arrays\n" +
					"indices length: " + rowIndices.length + "\n" +
					"value arrays length: " + realLength);
		}

		//Confirm realValues is actually an array
		Class<?> realClass = realValues.getClass();
		if (!realClass.isArray()) {
			throw new IllegalArgumentException("realValues is not an array, type: " + realClass.getCanonicalName());
		}

		//Confirm the realValues is of the supported type
		Class<?> requiredBaseComponentType = linearArrayType.getComponentType();
		Class<?> realBaseComponentType = ArrayUtils.getBaseComponentType(realClass);
		if (!realBaseComponentType.equals(requiredBaseComponentType)) {
			throw new IllegalArgumentException("realValues is not an array of the required class\n" +
					"Required base component type: " + requiredBaseComponentType.getCanonicalName() + "\n" +
					"Provided base component type: " + realBaseComponentType.getCanonicalName());
		}

		//Real and imag value arrays must have some characteristics in common
		if (imagValues != null) {
			if (!imagValues.getClass().equals(realClass)) {
				throw new IllegalArgumentException("imagValues is not of the same class as realValues\n" +
						"realValues class: " + realClass.getCanonicalName() + "\n" +
						"imagValues class: " + imagValues.getClass().getCanonicalName());
			}

			//If imag is provided, it must be the same length as real
			int imagLength = Array.getLength(imagValues);
			if (realLength != imagLength) {
				throw new IllegalArgumentException("realValues and imagValues must be the same length\n" +
						"realValues length: " + realLength + "\n" +
						"imagValues length: " + imagLength);
			}
		}
	}

	private static class SparseKey implements Comparable<SparseKey> {
		final int linearIndex;
		final int row;
		final int col;

		SparseKey(int row, int col, int numRows) {
			this.linearIndex = ArrayUtils.multidimensionalIndicesToLinearIndex(numRows, row, col);
			this.row = row;
			this.col = col;
		}

		@Override
		public int hashCode() {
			return linearIndex;
		}

		@Override
		public boolean equals(Object obj) {
			boolean equal = false;
			if (this == obj) {
				equal = true;
			} else if (obj != null && this.getClass().equals(obj.getClass())) {
				SparseKey otherKey = (SparseKey) obj;
				equal = (linearIndex == otherKey.linearIndex);
			}

			return equal;
		}

		@Override
		public int compareTo(SparseKey other) {
			return linearIndex - other.linearIndex;
		}
	}

	private static void setSparseValue(SparseValue value, Object realValues, Object imagValues, int index) {
		if (value instanceof ByteSparseValue) {
			((byte[]) realValues)[index] = ((ByteSparseValue) value).real;
			if (imagValues != null) {
				((byte[]) imagValues)[index] = ((ByteSparseValue) value).imag;
			}
		} else if (value instanceof ShortSparseValue) {
			((short[]) realValues)[index] = ((ShortSparseValue) value).real;
			if (imagValues != null) {
				((short[]) imagValues)[index] = ((ShortSparseValue) value).imag;
			}
		} else if (value instanceof IntSparseValue) {
			((int[]) realValues)[index] = ((IntSparseValue) value).real;
			if (imagValues != null) {
				((int[]) imagValues)[index] = ((IntSparseValue) value).imag;
			}
		} else if (value instanceof LongSparseValue) {
			((long[]) realValues)[index] = ((LongSparseValue) value).real;
			if (imagValues != null) {
				((long[]) imagValues)[index] = ((LongSparseValue) value).imag;
			}
		} else if (value instanceof FloatSparseValue) {
			((float[]) realValues)[index] = ((FloatSparseValue) value).real;
			if (imagValues != null) {
				((float[]) imagValues)[index] = ((FloatSparseValue) value).imag;
			}
		} else if (value instanceof DoubleSparseValue) {
			((double[]) realValues)[index] = ((DoubleSparseValue) value).real;
			if (imagValues != null) {
				((double[]) imagValues)[index] = ((DoubleSparseValue) value).imag;
			}
		} else if (value instanceof CharSparseValue) {
			((char[]) realValues)[index] = ((CharSparseValue) value).value;
		} else if (value instanceof BooleanSparseValue) {
			((boolean[]) realValues)[index] = ((BooleanSparseValue) value).value;
		} else {
			throw new IllegalArgumentException("Unsupported sparse value\n" +
					"value: " + value + "\n" +
					"realValues: " + realValues + "\n" +
					"imagValue: " + imagValues);
		}
	}

	private static SparseValue getSparseValue(Object realValues, Object imagValues, int index) {
		SparseValue value;
		if (realValues instanceof byte[]) {
			byte real = Array.getByte(realValues, index);
			byte imag = imagValues == null ? 0 : Array.getByte(imagValues, index);
			value = new ByteSparseValue(real, imag);
		} else if (realValues instanceof short[]) {
			short real = Array.getShort(realValues, index);
			short imag = imagValues == null ? 0 : Array.getShort(imagValues, index);
			value = new ShortSparseValue(real, imag);
		} else if (realValues instanceof int[]) {
			int real = Array.getInt(realValues, index);
			int imag = imagValues == null ? 0 : Array.getInt(imagValues, index);
			value = new IntSparseValue(real, imag);
		} else if (realValues instanceof long[]) {
			long real = Array.getLong(realValues, index);
			long imag = imagValues == null ? 0 : Array.getLong(imagValues, index);
			value = new LongSparseValue(real, imag);
		} else if (realValues instanceof float[]) {
			float real = Array.getFloat(realValues, index);
			float imag = imagValues == null ? 0 : Array.getFloat(imagValues, index);
			value = new FloatSparseValue(real, imag);
		} else if (realValues instanceof double[]) {
			double real = Array.getDouble(realValues, index);
			double imag = imagValues == null ? 0 : Array.getDouble(imagValues, index);
			value = new DoubleSparseValue(real, imag);
		} else if (realValues instanceof char[]) {
			char charValue = Array.getChar(realValues, index);
			value = new CharSparseValue(charValue);
		} else if (realValues instanceof boolean[]) {
			boolean booleanValue = Array.getBoolean(realValues, index);
			value = new BooleanSparseValue(booleanValue);
		} else {
			throw new IllegalArgumentException("Cannot create sparse value for array of type: " +
					realValues.getClass());
		}

		return value;
	}

	private static SparseValue addSparseValue(SparseValue value1, SparseValue value2) {
		SparseValue sum;
		if (value1 instanceof ByteSparseValue && value2 instanceof ByteSparseValue) {
			sum = ((ByteSparseValue) value1).add((ByteSparseValue) value2);
		} else if (value1 instanceof ShortSparseValue && value2 instanceof ShortSparseValue) {
			sum = ((ShortSparseValue) value1).add((ShortSparseValue) value2);
		} else if (value1 instanceof IntSparseValue && value2 instanceof IntSparseValue) {
			sum = ((IntSparseValue) value1).add((IntSparseValue) value2);
		} else if (value1 instanceof LongSparseValue && value2 instanceof LongSparseValue) {
			sum = ((LongSparseValue) value1).add((LongSparseValue) value2);
		} else if (value1 instanceof FloatSparseValue && value2 instanceof FloatSparseValue) {
			sum = ((FloatSparseValue) value1).add((FloatSparseValue) value2);
		} else if (value1 instanceof DoubleSparseValue && value2 instanceof DoubleSparseValue) {
			sum = ((DoubleSparseValue) value1).add((DoubleSparseValue) value2);
		} else if (value1 instanceof CharSparseValue && value2 instanceof CharSparseValue) {
			sum = ((CharSparseValue) value1).add((CharSparseValue) value2);
		} else if (value1 instanceof BooleanSparseValue && value2 instanceof BooleanSparseValue) {
			sum = ((BooleanSparseValue) value1).add((BooleanSparseValue) value2);
		} else {
			throw new IllegalArgumentException("Cannot add " + value1 + " and " + value2);
		}

		return sum;
	}

	private static interface SparseValue {
		boolean isDefaultValue();
	}

	private static class ByteSparseValue implements SparseValue {
		final byte real, imag;

		ByteSparseValue(byte real, byte imag) {
			this.real = real;
			this.imag = imag;
		}

		ByteSparseValue add(ByteSparseValue value) {
			return new ByteSparseValue((byte) (real + value.real), (byte) (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == (byte) 0) && (imag == (byte) 0);
		}
	}

	private static class ShortSparseValue implements SparseValue {
		final short real, imag;

		ShortSparseValue(short real, short imag) {
			this.real = real;
			this.imag = imag;
		}

		ShortSparseValue add(ShortSparseValue value) {
			return new ShortSparseValue((short) (real + value.real), (short) (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == (short) 0) && (imag == (short) 0);
		}
	}

	private static class IntSparseValue implements SparseValue {
		final int real, imag;

		IntSparseValue(int real, int imag) {
			this.real = real;
			this.imag = imag;
		}

		IntSparseValue add(IntSparseValue value) {
			return new IntSparseValue((real + value.real), (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == 0) && (imag == 0);
		}
	}

	private static class LongSparseValue implements SparseValue {
		final long real, imag;

		LongSparseValue(long real, long imag) {
			this.real = real;
			this.imag = imag;
		}

		LongSparseValue add(LongSparseValue value) {
			return new LongSparseValue((real + value.real), (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == 0L) && (imag == 0L);
		}
	}

	private static class FloatSparseValue implements SparseValue {
		final float real, imag;

		FloatSparseValue(float real, float imag) {
			this.real = real;
			this.imag = imag;
		}

		FloatSparseValue add(FloatSparseValue value) {
			return new FloatSparseValue((real + value.real), (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == 0f) && (imag == 0f);
		}
	}

	private static class DoubleSparseValue implements SparseValue {
		final double real, imag;

		DoubleSparseValue(double real, double imag) {
			this.real = real;
			this.imag = imag;
		}

		DoubleSparseValue add(DoubleSparseValue value) {
			return new DoubleSparseValue((real + value.real), (imag + value.imag));
		}

		@Override
		public boolean isDefaultValue() {
			return (real == 0d) && (imag == 0d);
		}
	}

	private static class CharSparseValue implements SparseValue {
		final char value;

		CharSparseValue(char value) {
			this.value = value;
		}

		CharSparseValue add(CharSparseValue value) {
			return new CharSparseValue((char) (this.value + value.value));
		}

		@Override
		public boolean isDefaultValue() {
			return (value == '\0');
		}
	}

	private static class BooleanSparseValue implements SparseValue {
		final boolean value;

		BooleanSparseValue(boolean value) {
			this.value = value;
		}

		BooleanSparseValue add(BooleanSparseValue value) {
			return new BooleanSparseValue(this.value || value.value);
		}

		@Override
		public boolean isDefaultValue() {
			return !value;
		}
	}
}
