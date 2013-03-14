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
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabDoubleSparseMatrix extends MatlabDoubleMatrix<double[][]>
{
    private final SparseArray<double[]> _array;
    
    MatlabDoubleSparseMatrix(int[] linearIndices, int[] rowIndices, int[] colIndices, double[] real, double[] imag,
            int numRows, int numCols)
    {
        _array = new SparseArray<double[]>(double[].class, linearIndices, rowIndices, colIndices, real, imag,
                numRows, numCols);
    }
    
    MatlabDoubleSparseMatrix(int[] rowIndices, int[] colIndices, double[] real, double[] imag, int numRows, int numCols)
    {
        _array = new SparseArray<double[]>(double[].class, rowIndices, colIndices, real, imag, numRows, numCols);
    }
    
    @Override
    BaseArray<double[], double[][]> getBaseArray()
    {
        return _array;
    }
    
    private double getRealElementAtSparseIndex(int sparseIndex)
    {
        double val = 0;
        if(sparseIndex >= 0)
        {
            val = _array._realValues[sparseIndex];
        }
        
        return val;
    }
    
    private double getImaginaryElementAtSparseIndex(int sparseIndex)
    {
        double val = 0;
        if(!_array.isReal() && sparseIndex >= 0)
        {
            val = _array._imagValues[sparseIndex];
        }
        
        return val;
    }
    
    
    
    @Override
    public double getRealElementAtLinearIndex(int linearIndex)
    {
        return this.getRealElementAtSparseIndex(_array.getSparseIndexForLinearIndex(linearIndex));
    }
    
    @Override
    public double getImaginaryElementAtLinearIndex(int linearIndex)
    {
        return this.getImaginaryElementAtSparseIndex(_array.getSparseIndexForLinearIndex(linearIndex));
    }
    
    
    
    @Override
    public double getRealElementAtIndices(int row, int column)
    {
        return this.getRealElementAtSparseIndex(_array.getSparseIndexForIndices(row, column));
    }
    
    @Override
    public double getRealElementAtIndices(int row, int column, int page)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
    }
    
    @Override
    public double getRealElementAtIndices(int row, int column, int[] pages)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
                (2 + pages.length) + " indices");
    }
    
    
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column)
    {
        return this.getImaginaryElementAtSparseIndex(_array.getSparseIndexForIndices(row, column));
    }
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column, int page)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
    }
    
    @Override
    public double getImaginaryElementAtIndices(int row, int column, int[] pages)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
                (2 + pages.length) + " indices");
    }
    
    
    
    @Override
    public MatlabDouble getElementAtLinearIndex(int linearIndex)
    {
        int sparseIndex = _array.getSparseIndexForLinearIndex(linearIndex);
        
        return new MatlabDouble(this.getRealElementAtSparseIndex(sparseIndex),
                this.getImaginaryElementAtSparseIndex(sparseIndex));
    }
    
    
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column)
    {
        int sparseIndex = _array.getSparseIndexForIndices(row, column);
        
        return new MatlabDouble(this.getRealElementAtSparseIndex(sparseIndex),
                this.getImaginaryElementAtSparseIndex(sparseIndex));
    }
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column, int page)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using 3 indices");
    }
    
    @Override
    public MatlabDouble getElementAtIndices(int row, int column, int[] pages)
    {
        throw new IllegalArgumentException("Array has 2 dimensions, it cannot be indexed into using " +
                (2 + pages.length) + " indices");
    }
    
    @Override
    public int hashCode()
    {
        return _array.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        boolean equal = false;
        if(this == obj)
        {
            equal = true;
        }
        else if(obj != null && this.getClass().equals(obj.getClass()))
        {
            MatlabDoubleSparseMatrix other = (MatlabDoubleSparseMatrix) obj;
            equal = _array.equals(other._array);
        }
        
        return equal;
    }
}