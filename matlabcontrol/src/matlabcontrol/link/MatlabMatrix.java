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
