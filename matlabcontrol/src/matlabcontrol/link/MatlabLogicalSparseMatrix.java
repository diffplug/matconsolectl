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
