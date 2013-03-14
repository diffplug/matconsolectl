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
public abstract class MatlabDoubleMatrix<T> extends MatlabNumericMatrix<double[], T>
{
    MatlabDoubleMatrix() { }
    
    public static <T> MatlabDoubleMatrix<T> getFull(T real, T imag)
    {
        return new MatlabDoubleFullMatrix<T>(real, imag);
    }
    
    public static MatlabDoubleMatrix<double[][]> getSparse(int[] rowIndices, int[] colIndices,
            double[] real, double[] imag, int numRows, int numCols)
    {
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
    public abstract MatlabDouble getElementAtIndices(int row, int column, int[] pages);
}