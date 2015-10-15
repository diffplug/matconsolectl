/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions for working with and creating arrays. This class contains some of the functionality contained in
 * {@link java.lang.Arrays}, but designed to work when the type of the array is not statically known.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayUtils {
	private ArrayUtils() {}

	/**
	 * Deeply copies a primitive array. If the array is not primitive then the {@code Object}s in the array will not
	 * be copies, although the arrays will be copied.
	 * 
	 * @param <T>
	 * @param array
	 * @return copy of array
	 */
	@SuppressWarnings("unchecked")
	static <T> T deepCopy(T array) {
		T copy;

		if (array == null) {
			copy = null;
		} else if (array.getClass().isArray()) {
			//Array of arrays
			if (array.getClass().getComponentType().isArray()) {
				int arrayLength = Array.getLength(array);
				copy = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength);
				for (int i = 0; i < arrayLength; i++) {
					Array.set(copy, i, deepCopy(Array.get(array, i)));
				}
			}
			//Array of values
			else {
				int arrayLength = Array.getLength(array);
				copy = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength);
				System.arraycopy(array, 0, copy, 0, arrayLength);
			}
		} else {
			throw new IllegalArgumentException("Input not an array: " + array.getClass().getCanonicalName());
		}

		return copy;
	}

	/**
	 * Computes the total number of elements in an array with dimensions specified by {@code dimensions}.
	 * 
	 * @param dimensions
	 * @return 
	 */
	static int getNumberOfElements(int[] dimensions) {
		int size = 0;

		for (int length : dimensions) {
			if (size == 0) {
				size = length;
			} else if (length != 0) {
				size *= length;
			}
		}

		return size;
	}

	/**
	 * Multidimensional indices to linear index. Similar to MATLAB's (@code sub2ind} function. The lengths of
	 * {@code dimensions} and {@code indices} should be the same; this is <b>not</b> checked.
	 * 
	 * @param dimensions the lengths of the array in each dimension
	 * @param indices
	 * @return linear index
	 */
	static int multidimensionalIndicesToLinearIndex(int[] dimensions, int[] indices) {
		int linearIndex = 0;

		int accumSize = 1;
		for (int i = 0; i < dimensions.length; i++) {
			linearIndex += accumSize * indices[i];
			accumSize *= dimensions[i];
		}

		return linearIndex;
	}

	//Optimized version for 2 indices
	static int multidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column) {
		return column * dimensions[0] + row;
	}

	static int multidimensionalIndicesToLinearIndex(int numRows, int row, int column) {
		return column * numRows + row;
	}

	//Optimized version for 3 indices
	static int multidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column, int page) {
		return page * (dimensions[0] * dimensions[1]) + column * dimensions[0] + row;
	}

	/**
	 * Multidimensional indices to linear index where there must be at least two indices {@code row} and {@code column}.
	 * The length of the {@code dimensions} and the indices must be the same, where the length of the indices is
	 * determined as {@code pages.length + 2}. Each index must be less than the length of the corresponding dimension.
	 * 
	 * @param dimensions the dimensions of the array
	 * @param row the row index
	 * @param column the column index
	 * @param pages the zero or more page indices
	 * @return 
	 * @throws IllegalArgumentException if the number of indices does not equal the number of dimensions
	 * @throws ArrayIndexOutOfBoundsException if the index is greater than or equal to the length of the corresponding
	 * dimension
	 */
	static int checkedMultidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column, int pages[]) {
		//Check the number of indices provided was correct
		if (dimensions.length != pages.length + 2) {
			throw new IllegalArgumentException("Array has " + dimensions.length + " dimension(s), it cannot be " +
					"indexed into using " + (pages.length + 2) + " indices");
		}

		//Check the row index is in bounds
		if (row >= dimensions[0]) {
			throw new ArrayIndexOutOfBoundsException("[" + row + "] is out of bounds for dimension 0 where the " +
					"length is " + dimensions[0]);
		}
		//Check the column index is in bounds
		if (column >= dimensions[1]) {
			throw new ArrayIndexOutOfBoundsException("[" + column + "] is out of bounds for dimension 1 where the " +
					"length is " + dimensions[1]);
		}
		//Check the page indices are in bounds
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] >= dimensions[i + 2]) {
				throw new ArrayIndexOutOfBoundsException("[" + pages[i] + "] is out of bounds for dimension " +
						(i + 2) + " where the length is " + dimensions[i + 2]);
			}
		}

		//Convert the indices to a linear index
		int linearIndex = 0;

		int accumSize = 1;

		//row
		linearIndex += accumSize * row;
		accumSize *= dimensions[0];

		//column
		linearIndex += accumSize * column;
		accumSize *= dimensions[1];

		//pages
		for (int i = 0; i < pages.length; i++) {
			linearIndex += accumSize * pages[i];
			accumSize *= dimensions[i + 2];
		}

		return linearIndex;
	}

	//Optimized for 2 indices
	static int checkedMultidimensionalIndicesToLinearIndex(int numRows, int numCols, int row, int column) {
		//Check the row index is in bounds
		if (row >= numRows) {
			throw new ArrayIndexOutOfBoundsException("[" + row + "] is out of bounds for dimension 0 where the " +
					"length is " + numRows);
		}
		//Check the column index is in bounds
		if (column >= numCols) {
			throw new ArrayIndexOutOfBoundsException("[" + column + "] is out of bounds for dimension 1 where the " +
					"length is " + numCols);
		}

		return column * numRows + row;
	}

	//Optimized for 2 indices
	static int checkedMultidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column) {
		//Check the row index is in bounds
		if (row >= dimensions[0]) {
			throw new ArrayIndexOutOfBoundsException("[" + row + "] is out of bounds for dimension 0 where the " +
					"length is " + dimensions[0]);
		}
		//Check the column index is in bounds
		if (column >= dimensions[1]) {
			throw new ArrayIndexOutOfBoundsException("[" + column + "] is out of bounds for dimension 1 where the " +
					"length is " + dimensions[1]);
		}

		return column * dimensions[0] + row;
	}

	//Optimized for 3 indices
	static int checkedMultidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column, int page) {
		//Check the row index is in bounds
		if (row >= dimensions[0]) {
			throw new ArrayIndexOutOfBoundsException("[" + row + "] is out of bounds for dimension 0 where the " +
					"length is " + dimensions[0]);
		}
		//Check the column index is in bounds
		if (column >= dimensions[1]) {
			throw new ArrayIndexOutOfBoundsException("[" + column + "] is out of bounds for dimension 1 where the " +
					"length is " + dimensions[1]);
		}
		//Check the page index is in bounds
		if (page >= dimensions[2]) {
			throw new ArrayIndexOutOfBoundsException("[" + column + "] is out of bounds for dimension 2 where the " +
					"length is " + dimensions[2]);
		}

		return page * (dimensions[0] * dimensions[1]) + column * dimensions[0] + row;
	}

	/**
	 * Linear index to multidimensional indices. Similar to MATLAB's (@code ind2sub} function.
	 * 
	 * @param dimensions the lengths of the array in each dimension
	 * @param linearIndex
	 * @return 
	 */
	static int[] linearIndexToMultidimensionalIndices(int[] dimensions, int linearIndex) {
		int[] indices = new int[dimensions.length];

		if (dimensions.length == 1) {
			indices[0] = linearIndex;
		} else {
			int pageSize = dimensions[0] * dimensions[1];
			int pageNumber = linearIndex / pageSize;

			//Row and column
			int indexInPage = linearIndex % pageSize;
			indices[0] = indexInPage % dimensions[0];
			indices[1] = indexInPage / dimensions[0];

			//3rd dimension and above
			int accumSize = 1;
			for (int dim = 2; dim < dimensions.length; dim++) {
				indices[dim] = (pageNumber / accumSize) % dimensions[dim];
				accumSize *= dimensions[dim];
			}
		}

		return indices;
	}

	/**
	 * Gets the base component type of {@code type} assuming {@code type} is a type of array. If {@code type} is not
	 * an array then the same class passed in will be returned. For example, if {@code type} is {@code boolean[][]} then
	 * {@code boolean} would be returned.
	 * 
	 * @param type
	 * @return 
	 */
	static Class<?> getBaseComponentType(Class<?> type) {
		while (type.isArray()) {
			type = type.getComponentType();
		}

		return type;
	}

	/**
	 * Gets the number of dimensions represented by {@code type}. If not an array then {@code 0} will be returned.
	 * 
	 * @param type
	 * @return 
	 */
	static int getNumberOfDimensions(Class<?> type) {
		int numDim = 0;
		while (type.isArray()) {
			numDim++;
			type = type.getComponentType();
		}

		return numDim;
	}

	/**
	 * Determines the maximum length for each dimension of the array.
	 * 
	 * @param array
	 * @return 
	 */
	static int[] computeBoundingDimensions(Object array) {
		int[] maxLengths = new int[getNumberOfDimensions(array.getClass())];

		//The length of this array
		int arrayLength = Array.getLength(array);
		maxLengths[0] = arrayLength;

		//If the array holds arrays as its entries
		if (array.getClass().getComponentType().isArray()) {
			//For each entry in the array
			for (int i = 0; i < arrayLength; i++) {
				//childLengths' information will be one index ahead of maxLengths
				int[] childLengths = computeBoundingDimensions(Array.get(array, i));
				for (int j = 0; j < childLengths.length; j++) {
					maxLengths[j + 1] = Math.max(maxLengths[j + 1], childLengths[j]);
				}
			}
		}

		return maxLengths;
	}

	/**
	 * Gets the class representing an array of type {@code componentType} with the number of dimensions specified by
	 * {@code rank}. JVMs typically impose a limit of 255 dimensions.
	 * 
	 * @param componentType
	 * @param rank
	 * @return 
	 */
	static Class<?> getArrayClass(Class<?> componentType, int rank) {
		String binaryName;
		if (componentType.isPrimitive()) {
			binaryName = getPrimitiveArrayBinaryName(componentType, rank);
		} else {
			binaryName = getObjectArrayBinaryName(componentType, rank);
		}

		try {
			return Class.forName(binaryName, false, componentType.getClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not create array class\n" +
					"Component Type (Canonical Name): " + componentType.getCanonicalName() + "\n" +
					"Component Type (Name): " + componentType.getName() + "\n" +
					"Rank: " + rank + "\n" +
					"Array Class Binary Name: " + binaryName, e);
		}
	}

	private static String getPrimitiveArrayBinaryName(Class<?> componentType, int rank) {
		//Build binary name
		char[] nameChars = new char[rank + 1];
		for (int i = 0; i < rank; i++) {
			nameChars[i] = '[';
		}
		nameChars[nameChars.length - 1] = PRIMITIVE_TO_BINARY_NAME.get(componentType);

		return new String(nameChars);
	}

	private static final Map<Class<?>, Character> PRIMITIVE_TO_BINARY_NAME;

	static {
		Map<Class<?>, Character> map = new HashMap<Class<?>, Character>();

		map.put(byte.class, 'B');
		map.put(short.class, 'S');
		map.put(int.class, 'I');
		map.put(long.class, 'J');
		map.put(float.class, 'F');
		map.put(double.class, 'D');
		map.put(boolean.class, 'Z');
		map.put(char.class, 'C');

		PRIMITIVE_TO_BINARY_NAME = Collections.unmodifiableMap(map);
	}

	private static String getObjectArrayBinaryName(Class<?> componentType, int rank) {
		String componentName = componentType.getName();
		int componentNameLength = componentName.length();

		char[] nameChars = new char[componentNameLength + rank + 2];
		for (int i = 0; i < rank; i++) {
			nameChars[i] = '[';
		}
		nameChars[rank] = 'L';
		System.arraycopy(componentName.toCharArray(), 0, nameChars, rank + 1, componentNameLength);
		nameChars[nameChars.length - 1] = ';';

		return new String(nameChars);
	}

	/**
	 * Hashes an array. Provides a deep hash code in the case of an object array that contains other arrays.
	 * 
	 * @param array must be an array of any type or {@code null}
	 * @return hashCode
	 */
	static int hashCode(Object array) {
		int hashCode;
		if (array == null) {
			hashCode = 0;
		} else if (array instanceof byte[]) {
			hashCode = Arrays.hashCode((byte[]) array);
		} else if (array instanceof short[]) {
			hashCode = Arrays.hashCode((short[]) array);
		} else if (array instanceof int[]) {
			hashCode = Arrays.hashCode((int[]) array);
		} else if (array instanceof long[]) {
			hashCode = Arrays.hashCode((long[]) array);
		} else if (array instanceof float[]) {
			hashCode = Arrays.hashCode((float[]) array);
		} else if (array instanceof double[]) {
			hashCode = Arrays.hashCode((double[]) array);
		} else if (array instanceof boolean[]) {
			hashCode = Arrays.hashCode((boolean[]) array);
		} else if (array instanceof char[]) {
			hashCode = Arrays.hashCode((char[]) array);
		}
		//This is true if array is any non-primitive array
		else if (array instanceof Object[]) {
			hashCode = Arrays.hashCode((Object[]) array);
		} else {
			throw new IllegalArgumentException("value provided is not array, class: " +
					array.getClass().getCanonicalName());
		}

		return hashCode;
	}

	static boolean equals(Object array1, Object array2) {
		boolean equal = false;
		//If the same array or both null
		if (array1 == array2) {
			equal = true;
		}
		//If both non-null and of the same class
		else if (array1 != null && array2 != null && array1.getClass().equals(array2.getClass())) {
			if (array1 instanceof byte[] && array2 instanceof byte[]) {
				equal = Arrays.equals((byte[]) array1, (byte[]) array2);
			} else if (array1 instanceof short[] && array2 instanceof short[]) {
				equal = Arrays.equals((short[]) array1, (short[]) array2);
			} else if (array1 instanceof int[] && array2 instanceof int[]) {
				equal = Arrays.equals((int[]) array1, (int[]) array2);
			} else if (array1 instanceof long[] && array2 instanceof long[]) {
				equal = Arrays.equals((long[]) array1, (long[]) array2);
			} else if (array1 instanceof float[] && array2 instanceof float[]) {
				equal = Arrays.equals((float[]) array1, (float[]) array2);
			} else if (array1 instanceof double[] && array2 instanceof double[]) {
				equal = Arrays.equals((double[]) array1, (double[]) array2);
			} else if (array1 instanceof boolean[] && array2 instanceof boolean[]) {
				equal = Arrays.equals((boolean[]) array1, (boolean[]) array2);
			} else if (array1 instanceof char[] && array2 instanceof char[]) {
				equal = Arrays.equals((char[]) array1, (char[]) array2);
			} else if (array1 instanceof Object[] && array2 instanceof Object[]) {
				equal = Arrays.equals((Object[]) array1, (Object[]) array2);
			} else {
				throw new IllegalArgumentException("One or more of the values provided are not an array\n" +
						"array1 class: " + array1.getClass() + "\n" +
						"array2 class: " + array2.getClass());
			}
		}

		return equal;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static boolean containsNonDefaultValue(Object array) {
		boolean contains;
		if (array == null) {
			throw new NullPointerException("array may not be null");
		} else if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("value provided is not an array, class: " + array.getClass());
		} else {
			ArrayContainmentOperation operation;
			if (array.getClass().getComponentType().isPrimitive()) {
				operation = CONTAINMENT_OPERATIONS.get(array.getClass().getComponentType());
			} else {
				operation = CONTAINMENT_OPERATIONS.get(Object[].class);
			}

			contains = operation.containsNonDefaultValue(array);
		}

		return contains;
	}

	private static final Map<Class<?>, ArrayContainmentOperation<?>> CONTAINMENT_OPERATIONS;

	static {
		Map<Class<?>, ArrayContainmentOperation<?>> map = new HashMap<Class<?>, ArrayContainmentOperation<?>>();

		map.put(byte[].class, new ByteArrayContainmentOperation());
		map.put(short[].class, new ShortArrayContainmentOperation());
		map.put(int[].class, new IntArrayContainmentOperation());
		map.put(long[].class, new LongArrayContainmentOperation());
		map.put(float[].class, new FloatArrayContainmentOperation());
		map.put(double[].class, new DoubleArrayContainmentOperation());
		map.put(boolean[].class, new BooleanArrayContainmentOperation());
		map.put(char[].class, new CharArrayContainmentOperation());
		map.put(Object[].class, new ObjectArrayContainmentOperation());

		CONTAINMENT_OPERATIONS = Collections.unmodifiableMap(map);
	}

	private static interface ArrayContainmentOperation<T> {
		public boolean containsNonDefaultValue(T array);
	}

	private static class ByteArrayContainmentOperation implements ArrayContainmentOperation<byte[]> {
		private static final byte DEFAULT_VAL = (byte) 0;

		@Override
		public boolean containsNonDefaultValue(byte[] array) {
			boolean contains = false;
			for (byte val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class ShortArrayContainmentOperation implements ArrayContainmentOperation<short[]> {
		private static final short DEFAULT_VAL = (short) 0;

		@Override
		public boolean containsNonDefaultValue(short[] array) {
			boolean contains = false;
			for (short val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class IntArrayContainmentOperation implements ArrayContainmentOperation<int[]> {
		private static final int DEFAULT_VAL = 0;

		@Override
		public boolean containsNonDefaultValue(int[] array) {
			boolean contains = false;
			for (int val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class LongArrayContainmentOperation implements ArrayContainmentOperation<long[]> {
		private static final long DEFAULT_VAL = 0L;

		@Override
		public boolean containsNonDefaultValue(long[] array) {
			boolean contains = false;
			for (long val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class FloatArrayContainmentOperation implements ArrayContainmentOperation<float[]> {
		private static final float DEFAULT_VAL = 0f;

		@Override
		public boolean containsNonDefaultValue(float[] array) {
			boolean contains = false;
			for (float val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class DoubleArrayContainmentOperation implements ArrayContainmentOperation<double[]> {
		private static final double DEFAULT_VAL = 0d;

		@Override
		public boolean containsNonDefaultValue(double[] array) {
			boolean contains = false;
			for (double val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class BooleanArrayContainmentOperation implements ArrayContainmentOperation<boolean[]> {
		private static final boolean DEFAULT_VAL = false;

		@Override
		public boolean containsNonDefaultValue(boolean[] array) {
			boolean contains = false;
			for (boolean val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class CharArrayContainmentOperation implements ArrayContainmentOperation<char[]> {
		private static final char DEFAULT_VAL = '\0';

		@Override
		public boolean containsNonDefaultValue(char[] array) {
			boolean contains = false;
			for (char val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}

	private static class ObjectArrayContainmentOperation implements ArrayContainmentOperation<Object[]> {
		private static final Object DEFAULT_VAL = null;

		@Override
		public boolean containsNonDefaultValue(Object[] array) {
			boolean contains = false;
			for (Object val : array) {
				if (val != DEFAULT_VAL) {
					contains = true;
					break;
				}
			}

			return contains;
		}
	}
}
