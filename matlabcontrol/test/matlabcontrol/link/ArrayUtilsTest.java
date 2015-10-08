/*
 * Copyright (c) 2013, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
