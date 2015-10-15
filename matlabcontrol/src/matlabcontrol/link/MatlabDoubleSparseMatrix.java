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
class MatlabDoubleSparseMatrix extends MatlabDoubleMatrix<double[][]> {
	private final SparseArray<double[]> _array;

	MatlabDoubleSparseMatrix(int[] linearIndices, int[] rowIndices, int[] colIndices, double[] real, double[] imag,
			int numRows, int numCols) {
		_array = new SparseArray<double[]>(double[].class, linearIndices, rowIndices, colIndices, real, imag,
				numRows, numCols);
	}

	MatlabDoubleSparseMatrix(int[] rowIndices, int[] colIndices, double[] real, double[] imag, int numRows, int numCols) {
		_array = new SparseArray<double[]>(double[].class, rowIndices, colIndices, real, imag, numRows, numCols);
	}

	@Override
	BaseArray<double[], double[][]> getBaseArray() {
		return _array;
	}

	private double getRealElementAtSparseIndex(int sparseIndex) {
		double val = 0;
		if (sparseIndex >= 0) {
			val = _array._realValues[sparseIndex];
		}

		return val;
	}

	private double getImaginaryElementAtSparseIndex(int sparseIndex) {
		double val = 0;
		if (!_array.isReal() && sparseIndex >= 0) {
			val = _array._imagValues[sparseIndex];
		}

		return val;
	}

	@Override
	public double getRealElementAtLinearIndex(int linearIndex) {
		return this.getRealElementAtSparseIndex(_array.getSparseIndexForLinearIndex(linearIndex));
	}

	@Override
	public double getImaginaryElementAtLinearIndex(int linearIndex) {
		return this.getImaginaryElementAtSparseIndex(_array.getSparseIndexForLinearIndex(linearIndex));
	}

	@Override
	public double getRealElementAtIndices(int row, int column) {
		return this.getRealElementAtSparseIndex(_array.getSparseIndexForIndices(row, column));
	}

	@Override
	public double getRealElementAtIndices(int row, int column, int page) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
	}

	@Override
	public double getRealElementAtIndices(int row, int column, int[] pages) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
				(2 + pages.length) + " indices");
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column) {
		return this.getImaginaryElementAtSparseIndex(_array.getSparseIndexForIndices(row, column));
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column, int page) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column, int[] pages) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
				(2 + pages.length) + " indices");
	}

	@Override
	public MatlabDouble getElementAtLinearIndex(int linearIndex) {
		int sparseIndex = _array.getSparseIndexForLinearIndex(linearIndex);

		return new MatlabDouble(this.getRealElementAtSparseIndex(sparseIndex),
				this.getImaginaryElementAtSparseIndex(sparseIndex));
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column) {
		int sparseIndex = _array.getSparseIndexForIndices(row, column);

		return new MatlabDouble(this.getRealElementAtSparseIndex(sparseIndex),
				this.getImaginaryElementAtSparseIndex(sparseIndex));
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column, int page) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column, int... pages) {
		throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
				(2 + pages.length) + " indices");
	}

	@Override
	public int hashCode() {
		return _array.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		boolean equal = false;
		if (this == obj) {
			equal = true;
		} else if (obj != null && this.getClass().equals(obj.getClass())) {
			MatlabDoubleSparseMatrix other = (MatlabDoubleSparseMatrix) obj;
			equal = _array.equals(other._array);
		}

		return equal;
	}
}
