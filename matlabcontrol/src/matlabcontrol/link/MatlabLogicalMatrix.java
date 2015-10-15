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
public abstract class MatlabLogicalMatrix<T> extends MatlabNonNumericMatrix<boolean[], T> {
	MatlabLogicalMatrix() {}

	public static <T> MatlabLogicalMatrix<T> getFull(T values) {
		return new MatlabLogicalFullMatrix<T>(values);
	}

	public static MatlabLogicalMatrix<boolean[][]> getSparse(int[] rowIndices, int[] colIndices, boolean[] values,
			int numRows, int numCols) {
		return new MatlabLogicalSparseMatrix(rowIndices, colIndices, values, numRows, numCols);
	}

	public abstract boolean getElementAtLinearIndex(int linearIndex);

	public abstract boolean getElementAtIndices(int row, int column);

	public abstract boolean getElementAtIndices(int row, int column, int page);

	public abstract boolean getElementAtIndices(int row, int column, int[] pages);
}
