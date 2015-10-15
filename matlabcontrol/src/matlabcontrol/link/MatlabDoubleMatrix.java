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
public abstract class MatlabDoubleMatrix<T> extends MatlabNumericMatrix<double[], T> {
	MatlabDoubleMatrix() {}

	public static <T> MatlabDoubleMatrix<T> getFull(T real, T imag) {
		return new MatlabDoubleFullMatrix<T>(real, imag);
	}

	public static MatlabDoubleMatrix<double[][]> getSparse(int[] rowIndices, int[] colIndices,
			double[] real, double[] imag, int numRows, int numCols) {
		return new MatlabDoubleSparseMatrix(rowIndices, colIndices, real, imag, numRows, numCols);
	}

	public abstract double getRealElementAtLinearIndex(int linearIndex);

	public abstract double getImaginaryElementAtLinearIndex(int linearIndex);

	public abstract double getRealElementAtIndices(int row, int column);

	public abstract double getRealElementAtIndices(int row, int column, int page);

	public abstract double getRealElementAtIndices(int row, int column, int[] pages);

	public abstract double getImaginaryElementAtIndices(int row, int column);

	public abstract double getImaginaryElementAtIndices(int row, int column, int page);

	public abstract double getImaginaryElementAtIndices(int row, int column, int[] pages);

	@Override
	public abstract MatlabDouble getElementAtLinearIndex(int linearIndex);

	@Override
	public abstract MatlabDouble getElementAtIndices(int row, int column);

	@Override
	public abstract MatlabDouble getElementAtIndices(int row, int column, int page);

	@Override
	public abstract MatlabDouble getElementAtIndices(int row, int column, int... pages);
}
