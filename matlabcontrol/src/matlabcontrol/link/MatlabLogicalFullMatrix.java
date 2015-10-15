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
class MatlabLogicalFullMatrix<T> extends MatlabLogicalMatrix<T> {
	private final FullArray<boolean[], T> _array;

	MatlabLogicalFullMatrix(boolean[] values, int[] dimensions) {
		_array = new FullArray<boolean[], T>(boolean[].class, values, null, dimensions);
	}

	MatlabLogicalFullMatrix(T values) {
		_array = new FullArray<boolean[], T>(boolean[].class, values, null);
	}

	@Override
	BaseArray<boolean[], T> getBaseArray() {
		return _array;
	}

	@Override
	public boolean getElementAtLinearIndex(int linearIndex) {
		return _array._real[linearIndex];
	}

	@Override
	public boolean getElementAtIndices(int row, int column) {
		return _array._real[_array.getLinearIndex(row, column)];
	}

	@Override
	public boolean getElementAtIndices(int row, int column, int page) {
		return _array._real[_array.getLinearIndex(row, column, page)];
	}

	@Override
	public boolean getElementAtIndices(int row, int column, int[] pages) {
		return _array._real[_array.getLinearIndex(row, column, pages)];
	}
}
