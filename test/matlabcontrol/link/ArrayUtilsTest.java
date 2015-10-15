/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;

public class ArrayUtilsTest {
	@Test
	public void testDeepCopyOnPrimitives() {
		byte[] bytes = new byte[]{1, 2, 3, 4, 5};
		byte[] bytesCopied = ArrayUtils.deepCopy(bytes);
		Assert.assertTrue(bytes != bytesCopied);
		Assert.assertArrayEquals(bytes, bytesCopied);

		short[] shorts = new short[]{1, 2, 3, 4, 5};
		short[] shortsCopied = ArrayUtils.deepCopy(shorts);
		Assert.assertTrue(shorts != shortsCopied);
		Assert.assertArrayEquals(shorts, shortsCopied);

		int[] ints = new int[]{1, 2, 3, 4, 5};
		int[] intsCopied = ArrayUtils.deepCopy(ints);
		Assert.assertTrue(ints != intsCopied);
		Assert.assertArrayEquals(ints, intsCopied);

		long[] longs = new long[]{1, 2, 3, 4, 5};
		long[] longsCopied = ArrayUtils.deepCopy(longs);
		Assert.assertTrue(longs != longsCopied);
		Assert.assertArrayEquals(longs, longsCopied);

		float[] floats = new float[]{1, 2, 3, 4, 5};
		float[] floatsCopied = ArrayUtils.deepCopy(floats);
		Assert.assertTrue(floats != floatsCopied);
		Assert.assertArrayEquals(floats, floatsCopied, 0.01f);

		double[] doubles = new double[]{1, 2, 3, 4, 5};
		double[] doublesCopied = ArrayUtils.deepCopy(doubles);
		Assert.assertTrue(doubles != doublesCopied);
		Assert.assertArrayEquals(doubles, doublesCopied, 0.01);

		boolean[] bools = new boolean[]{true, true, false, true};
		boolean[] boolsCopied = ArrayUtils.deepCopy(bools);
		Assert.assertTrue(bools != boolsCopied);
		Assert.assertArrayEquals(bools, boolsCopied);

		char[] chars = new char[]{1, 2, 3, 4, 5};
		char[] charsCopied = ArrayUtils.deepCopy(chars);
		Assert.assertTrue(chars != charsCopied);
		Assert.assertArrayEquals(chars, charsCopied);
	}

	public static void assertArraysEqual(Object expected, Object actual) {
		if (expected.getClass().isArray()) {
			if (!ArrayUtils.equals(expected, actual)) {
				throw new ComparisonFailure("test failed", toString(expected), toString(actual));
			}
		} else {
			if (!expected.equals(actual)) {
				throw new ComparisonFailure("test failed", toString(expected), toString(actual));
			}
		}
	}

	private static String toString(Object any) {
		return "class: " + any.getClass() + "\ntoString: " + any.toString();
	}
}
