/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabDoubleFullMatrix<T> extends MatlabDoubleMatrix<T> {
	private final FullArray<double[], T> _array;

	MatlabDoubleFullMatrix(double[] real, double[] imag, int[] dimensions) {
		_array = new FullArray<double[], T>(double[].class, real, imag, dimensions);
	}

	MatlabDoubleFullMatrix(T real, T imag) {
		_array = new FullArray<double[], T>(double[].class, real, imag);
	}

	@Override
	BaseArray<double[], T> getBaseArray() {
		return _array;
	}

	@Override
	public double getRealElementAtLinearIndex(int linearIndex) {
		return _array._real[linearIndex];
	}

	@Override
	public double getImaginaryElementAtLinearIndex(int linearIndex) {
		return _array._imag == null ? 0 : _array._imag[linearIndex];
	}

	@Override
	public double getRealElementAtIndices(int row, int column) {
		return _array._real[_array.getLinearIndex(row, column)];
	}

	@Override
	public double getRealElementAtIndices(int row, int column, int page) {
		return _array._real[_array.getLinearIndex(row, column, page)];
	}

	@Override
	public double getRealElementAtIndices(int row, int column, int[] pages) {
		return _array._real[_array.getLinearIndex(row, column, pages)];
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column) {
		return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column)];
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column, int page) {
		return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column, page)];
	}

	@Override
	public double getImaginaryElementAtIndices(int row, int column, int[] pages) {
		return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column, pages)];
	}

	@Override
	public MatlabDouble getElementAtLinearIndex(int linearIndex) {
		return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column) {
		int linearIndex = _array.getLinearIndex(row, column);

		return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column, int page) {
		int linearIndex = _array.getLinearIndex(row, column, page);

		return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
	}

	@Override
	public MatlabDouble getElementAtIndices(int row, int column, int... pages) {
		int linearIndex = _array.getLinearIndex(row, column, pages);

		return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
	}
}
