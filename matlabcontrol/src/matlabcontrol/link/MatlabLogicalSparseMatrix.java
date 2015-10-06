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
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabLogicalSparseMatrix extends MatlabLogicalMatrix<boolean[][]> {
	private final SparseArray<boolean[]> _array;

	MatlabLogicalSparseMatrix(int[] linearIndices, int[] rowIndices, int[] colIndices, boolean[] values,
			int numRows, int numCols) {
		_array = new SparseArray<boolean[]>(boolean[].class, linearIndices, rowIndices, colIndices, values, null,
				numRows, numCols);
	}

	MatlabLogicalSparseMatrix(int[] rowIndices, int[] colIndices, boolean[] values, int numRows, int numCols) {
		_array = new SparseArray<boolean[]>(boolean[].class, rowIndices, colIndices, values, null, numRows, numCols);
	}

	@Override
	BaseArray<boolean[], boolean[][]> getBaseArray() {
		return _array;
	}

	private boolean getElementAtSparseIndex(int sparseIndex) {
		boolean val = false;
		if (sparseIndex >= 0) {
			val = _array._realValues[sparseIndex];
		}

		return val;
	}

	@Override
	public boolean getElementAtLinearIndex(int linearIndex) {
		return getElementAtSparseIndex(_array.getSparseIndexForLinearIndex(linearIndex));
	}

	@Override
	public boolean getElementAtIndices(int row, int column) {
		return getElementAtSparseIndex(_array.getSparseIndexForIndices(row, column));
	}

	@Override
	public boolean getElementAtIndices(int row, int column, int page) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
	}

	@Override
	public boolean getElementAtIndices(int row, int column, int[] pages) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
				(2 + pages.length) + " indices");
	}
}
