/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import static matlabcontrol.link.ArrayUtils.*;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.link.MatlabType.MatlabTypeSetter;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayLinearizer {
	static MatlabTypeSetter getSetter(Object array) {
		return new MultidimensionalPrimitiveArraySetter(array);
	}

	private static class MultidimensionalPrimitiveArraySetter implements MatlabTypeSetter {
		private static final long serialVersionUID = 7871972881461753065L;
		private final Object _linearArray;
		private final int[] _lengths;

		public MultidimensionalPrimitiveArraySetter(Object multidimensionalArray) {
			_lengths = computeBoundingDimensions(multidimensionalArray);
			_linearArray = linearize(multidimensionalArray, _lengths);
		}

		@Override
		public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException {
			ops.setVariable(variableName, this);

			StringBuilder command = new StringBuilder();
			if (_lengths.length == 1) {
				command.append(variableName + " = reshape(" + variableName + ".getLinearArray(), 1, " + _lengths[0] + ");");
			} else {
				command.append(variableName + " = reshape(" + variableName + ".getLinearArray()");
				for (int length : _lengths) {
					command.append(", " + length);
				}
				command.append(");");
			}

			ops.eval(command.toString());
		}

		@SuppressWarnings("unused")
		// called in MATLAB by the script above
		public Object getLinearArray() {
			return _linearArray;
		}
	}

	/**
	 * Creates a linearized version of the {@code array}.
	 * 
	 * @param array a multidimensional primitive array
	 * @param lengths the bounding lengths of the array
	 * @return linear array
	 */
	static Object linearize(Object array, int[] lengths) {
		if (array == null || !array.getClass().isArray()) {
			throw new RuntimeException("provided object is not an array");
		}

		//Base component type of the array
		Class<?> baseClass = getBaseComponentType(array.getClass());
		if (!baseClass.isPrimitive()) {
			throw new RuntimeException("array type is not a primitive, type: " + baseClass.getCanonicalName());
		}

		//Create linear array with size equal to that of the bounding lengths of the array
		int size = getNumberOfElements(lengths);
		Object linearArray = Array.newInstance(baseClass, size);

		//Fill linearArray with values from array
		@SuppressWarnings("rawtypes")
		ArrayFillOperation fillOperation = FILL_OPERATIONS.get(baseClass);
		linearize_internal(linearArray, array, fillOperation, lengths, 0, new int[lengths.length]);

		return linearArray;
	}

	/**
	 * Performs the linearization using recursion.
	 * 
	 * @param linearArray array to be filled
	 * @param srcArray source array
	 * @param fillOperation operation to read values from srcArray and write into linearArray
	 * @param lengths the lengths of the array for the initial array supplied before any recursion
	 * @param depth the level of recursion, initially {@code 0}
	 * @param indices must be the length of {@code lengths}
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void linearize_internal(Object linearArray, Object srcArray, ArrayFillOperation fillOperation,
			int[] lengths, int depth, int[] indices) {
		//Base case - array holding non-array elements
		if (!srcArray.getClass().getComponentType().isArray()) {
			fillOperation.fill(linearArray, srcArray, indices, lengths);
		} else {
			int arrayLength = Array.getLength(srcArray);
			for (int i = 0; i < arrayLength; i++) {
				indices[depth] = i;
				linearize_internal(linearArray, Array.get(srcArray, i), fillOperation, lengths, depth + 1, indices);
			}
		}
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
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class ShortArrayFillOperation implements ArrayFillOperation<short[]> {
		@Override
		public void fill(short[] dst, short[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class IntArrayFillOperation implements ArrayFillOperation<int[]> {
		@Override
		public void fill(int[] dst, int[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class LongArrayFillOperation implements ArrayFillOperation<long[]> {
		@Override
		public void fill(long[] dst, long[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class FloatArrayFillOperation implements ArrayFillOperation<float[]> {
		@Override
		public void fill(float[] dst, float[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class DoubleArrayFillOperation implements ArrayFillOperation<double[]> {
		@Override
		public void fill(double[] dst, double[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class BooleanArrayFillOperation implements ArrayFillOperation<boolean[]> {
		@Override
		public void fill(boolean[] dst, boolean[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}

	private static class CharArrayFillOperation implements ArrayFillOperation<char[]> {
		@Override
		public void fill(char[] dst, char[] src, int[] indices, int[] lengths) {
			for (int i = 0; i < src.length; i++) {
				indices[indices.length - 1] = i;
				int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
				dst[linearIndex] = src[i];
			}
		}
	}
}
