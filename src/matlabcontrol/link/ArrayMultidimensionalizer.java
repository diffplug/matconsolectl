/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import static matlabcontrol.link.ArrayUtils.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.link.MatlabType.MatlabTypeGetter;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@SuppressWarnings("rawtypes")
class ArrayMultidimensionalizer {
	static class PrimitiveArrayGetter implements MatlabTypeGetter<Object> {
		private static final long serialVersionUID = -3228683201238234004L;
		private int[] _lengths;
		private Object _array;

		private boolean _retreived = false;

		private final boolean _getRealPart;
		private final boolean _keepLinear;

		public PrimitiveArrayGetter(boolean realPart, boolean keepLinear) {
			_getRealPart = realPart;
			_keepLinear = keepLinear;
		}

		public boolean isRealPart() {
			return _getRealPart;
		}

		public int[] getLengths() {
			return _lengths;
		}

		/**
		 * May be null if this represents the imaginary part and the array has no imaginary values or the array was
		 * empty.
		 * 
		 * @return 
		 */
		@Override
		public Object retrieve() {
			//Validate
			if (!_retreived) {
				throw new IllegalStateException("array has not yet been retrieved");
			}

			if (_array != null && !_keepLinear) {
				return multidimensionalize(_array, _lengths);
			} else {
				return _array;
			}
		}

		@Override
		public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			//Retrieve lengths of array
			double[] size = (double[]) ops.returningEval("size(" + variableName + ");", 1)[0];
			_lengths = new int[size.length];
			for (int i = 0; i < size.length; i++) {
				_lengths[i] = (int) size[i];
			}

			//Retrieve array
			String name = (String) ops.returningEval("genvarname('" + variableName + "_getter', who);", 1)[0];
			ops.setVariable(name, this);
			try {
				if (_getRealPart) {
					ops.eval(name + ".setArray(reshape(" + variableName + ", 1, []));");
				} else {
					//If the array has an imaginary part, retrieve it
					boolean isReal = ((boolean[]) ops.returningEval("isreal(" + variableName + ");", 1)[0])[0];
					if (!isReal) {
						ops.eval(name + ".setArray(imag(reshape(" + variableName + ", 1, [])));");
					}
				}
			} finally {
				ops.eval("clear " + name);
			}

			_retreived = true;
		}

		//The following methods are to be called only from MATLAB to pass in the array

		public void setArray(byte[] array) {
			_array = array;
		}

		public void setArray(short[] array) {
			_array = array;
		}

		public void setArray(int[] array) {
			_array = array;
		}

		public void setArray(long[] array) {
			_array = array;
		}

		public void setArray(float[] array) {
			_array = array;
		}

		public void setArray(double[] array) {
			_array = array;
		}

		public void setArray(char[] array) {
			_array = array;
		}

		public void setArray(boolean[] array) {
			_array = array;
		}
	}

	/**
	 * Creates a rectangular multidimensional array from {@code linearArray} with the length of each dimension specified
	 * by {@code lengths}. The length of the array must equal the total size specified by {@code lengths}.
	 * 
	 * @param <T>
	 * @param linearArray a primitive linear array
	 * @param lengths length of each dimension
	 * @return multidimensional array
	 */
	static Object multidimensionalize(Object linearArray, int[] lengths) {
		//Validate primitive single dimension array
		if (linearArray == null || !linearArray.getClass().isArray() &&
				!linearArray.getClass().getComponentType().isPrimitive()) {
			throw new RuntimeException("linear array must be a single dimension primitive array");
		}

		//Validate lengths
		if (getNumberOfElements(lengths) != Array.getLength(linearArray)) {
			throw new RuntimeException("linear array's length does not match the total size of the provided " +
					"multidimensional lengths\n" +
					"Linear Array Length: " + Array.getLength(linearArray) + "\n" +
					"Multidimensional Lengths: " + Arrays.toString(lengths) + "\n" +
					"Multidimensional Total Size: " + getNumberOfElements(lengths));
		}

		Class componentType = linearArray.getClass().getComponentType();
		Class outputArrayType = getArrayClass(componentType, lengths.length);
		ArrayFillOperation fillOperation = FILL_OPERATIONS.get(componentType);

		return multidimensionalize_internal(linearArray, outputArrayType, fillOperation, lengths, 0, new int[lengths.length]);
	}

	/**
	 * The real logic of the multidimensionalize. This method recurs on itself, hence the need for the extra parameters.
	 * This method does not store state in any external variables.
	 * 
	 * @param linearArray
	 * @param outputArrayType
	 * @param fillOperation operation used to fill in single dimension pieces of the array
	 * @param lengths
	 * @param depth should be {@code 0} initially
	 * @param indices must be the length of {@code lengths}
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Object multidimensionalize_internal(Object linearArray, Class<?> outputArrayType,
			ArrayFillOperation fillOperation, int[] lengths, int depth, int[] indices) {
		Class<?> componentType = outputArrayType.getComponentType();
		int arrayLength = lengths[depth];
		Object array = Array.newInstance(componentType, arrayLength);

		//If what was created holds an array, then fill it
		if (componentType.isArray()) {
			//If the array that was created holds the primitive array: double[], int[], boolean[] etc.
			if (componentType.getComponentType().isPrimitive()) {
				//Iterate over the created array, placing a primitive array in each index
				for (int i = 0; i < arrayLength; i++) {
					indices[indices.length - 2] = i;

					//Create the primitive array
					Object primitiveArray = Array.newInstance(componentType.getComponentType(), lengths[lengths.length - 1]);

					//Fill the primitive array with values from the linear array
					fillOperation.fill(primitiveArray, linearArray, indices, lengths);

					//Place primitive array into the enclosing array
					Array.set(array, i, primitiveArray);
				}
			} else {
				//Iterate over the created array, placing an array in each index (using recursion)
				for (int i = 0; i < arrayLength; i++) {
					indices[depth] = i;

					Object innerArray = multidimensionalize_internal(linearArray, componentType,
							fillOperation, lengths, depth + 1, indices);
					Array.set(array, i, innerArray);
				}
			}
		}
		//If the componentType is not an array, then the original request was to create a one dimensional array
		else {
			System.arraycopy(linearArray, 0, array, 0, arrayLength);
		}

		return array;
	}

	private static final Map<Class<?>, ArrayFillOperation<?>> FILL_OPERATIONS;

	static {
		Map<Class<?>, ArrayFillOperation<?>> map = new HashMap<Class<?>, ArrayFillOperation<?>>();

		map.put(byte.class, new ByteArrayFillOperation());
		map.put(short.class, new ShortArrayFillOperation());
		map.put(int.class, new IntArrayFillOperation());
		map.put(long.class, new LongArrayFillOperation());
		map.put(float.class, new FloatArrayFillOperation());
		map.put(double.class, new DoubleArrayFillOperation());
		map.put(boolean.class, new BooleanArrayFillOperation());
		map.put(char.class, new CharArrayFillOperation());

		FILL_OPERATIONS = Collections.unmodifiableMap(map);
	}

	private static interface ArrayFillOperation<T> {
		public void fill(T dst, T src, int[] indices, int[] lengths);
	}

	private static class ByteArrayFillOperation implements ArrayFillOperation<byte[]> {
		@Override
		public void fill(byte[] dst, byte[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class ShortArrayFillOperation implements ArrayFillOperation<short[]> {
		@Override
		public void fill(short[] dst, short[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class IntArrayFillOperation implements ArrayFillOperation<int[]> {
		@Override
		public void fill(int[] dst, int[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class LongArrayFillOperation implements ArrayFillOperation<long[]> {
		@Override
		public void fill(long[] dst, long[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class FloatArrayFillOperation implements ArrayFillOperation<float[]> {
		@Override
		public void fill(float[] dst, float[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class DoubleArrayFillOperation implements ArrayFillOperation<double[]> {
		@Override
		public void fill(double[] dst, double[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class BooleanArrayFillOperation implements ArrayFillOperation<boolean[]> {
		@Override
		public void fill(boolean[] dst, boolean[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}

	private static class CharArrayFillOperation implements ArrayFillOperation<char[]> {
		@Override
		public void fill(char[] dst, char[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < dst.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[i] = src[linearIndex];
			}
		}
	}
}
