package matlabcontrol.link;

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

/**
 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabDoubleDenseMatrix<T> extends MatlabDoubleMatrix<T>
{
    private final DenseArray<double[], T> _array;
    
    MatlabDoubleDenseMatrix(double[] real, double[] imag, int[] dimensions)
    {
        _array = new DenseArray<double[], T>(double[].class, real, imag, dimensions);
    }
    
    MatlabDoubleDenseMatrix(T real, T imag)
    {
        _array = new DenseArray<double[], T>(double[].class, real, imag);
    }
    
    @Override
    BaseArray<double[], T> getBaseArray()
    {
        return _array;
    }
    
    @Override
    public double getRealElementAtLinearIndex(int linearIndex)
    {
        return _array._real[linearIndex];
    }
    
    @Override
    public double getImaginaryElementAtLinearIndex(int linearIndex)
    {
        return _array._imag == null ? 0 : _array._imag[linearIndex];
    }
    
    
    
    @Override
    public double getRealElementAtIndices(int row, int column)
    {
        return _array._real[_array.getLinearIndex(row, column)];
    }
    
    @Override
    public double getRealElementAtIndices(int row, int column, int page)
    {
        return _array._real[_array.getLinearIndex(row, column, page)];
    }
    
    @Override
    public double getRealElementAtIndices(int row, int column, int[] pages)
    {
        return _array._real[_array.getLinearIndex(row, column, pages)];
    }
    
    
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column)
    {
        return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column)];
    }
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column, int page)
    {
        return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column, page)];
    }
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column, int[] pages)
    {
        return _array._imag == null ? 0 : _array._imag[_array.getLinearIndex(row, column, pages)];
    }
    
    
    
    @Override
    public MatlabDouble getElementAtLinearIndex(int linearIndex)
    {
        return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
    }
    
    
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column)
    {
        int linearIndex = _array.getLinearIndex(row, column);
        
        return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
    }
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column, int page)
    {
        int linearIndex = _array.getLinearIndex(row, column, page);
        
        return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
    }
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column, int[] pages)
    {
        int linearIndex = _array.getLinearIndex(row, column, pages);
        
        return new MatlabDouble(_array._real[linearIndex], _array._imag == null ? 0 : _array._imag[linearIndex]);
    }
}