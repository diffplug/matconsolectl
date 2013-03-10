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

import java.util.Arrays;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * 
 * @param <L> underlying linear array - single dimensional array type, ex. {@code byte[]}
 * @param <T> output array - primitive numeric array type, ex. {@code byte[][][]}
 *            (1 or more dimensions is acceptable, including for example {@code byte[]})
 */
class SparseArray<L, T> extends BaseArray<L, T>
{
    /*
     * MATLAB's find(array) returns the linear indices of non-zero values
     * 
     * MATLAB's nonzeros(array) returns a column vector of the non-zero values
     */
    
    /**
     * The values in this array are the linear indices in the sparse array that have values. The index of an element
     * in this array maps to the index in {@code #_real} and {@code #_imag} that contain the value at the linear index
     * represented by the element.
     */
    private final int[] _indices;
    
    /**
     * The linear array of real values.
     */
    final L _real;
    
    /**
     * The linear array of imaginary values. Can be {@code null} if this number array is real.
     */
    final L _imag;
    
    /**
     * Caches the hash code.
     * <br><br>
     * To avoid any form of inter-thread communication this value may in the most degenerate case be recomputed for each
     * thread.
     */
    private Integer _hashCode = null;
    
    /**
     * Data from MATLAB. Provided as the indices, linear arrays of values, and dimensions.
     * 
     * @param linearArrayType
     * @param indices need to be sorted in ascending value
     * @param realLinear
     * @param imagLinear
     * @param dimensions 
     */
    SparseArray(Class<L> linearArrayType, int[] indices, L real, L imag, int[] dimensions)
    {
        super(linearArrayType, dimensions);
        
        //Linear indices
        _indices = indices;
        
        //The real and imaginary arrays should always be of type L, but validate it
        _real = linearArrayType.cast(real);
        _imag = linearArrayType.cast(imag);
    }
    
    @Override
    boolean isReal()
    {
        //For a sparse array, if there are no imaginary values then it is real. There is no need to check if all
        //entries of the imaginary array are zero as zero entries are not stored for sparse arrays.
        
        return (_imag == null);
    }
    
    @Override
    T toRealArray()
    {
        throw new UnsupportedOperationException("not yet implemented");
    }
    
    @Override
    T toImaginaryArray()
    {
        throw new UnsupportedOperationException("not yet implemented");
    }
    
    @Override
    boolean isSparse()
    {
        return true;
    }
    
    /**
     * Converts from a linear index for the array to the corresponding index in {@link #_real} and {@link #_imag}. If
     * no corresponding index was found then the value returned will be negative.
     * 
     * @param linearIndex
     * @return sparse index
     */
    int getSparseIndexForLinearIndex(int linearIndex)
    {
        return Arrays.binarySearch(_indices, linearIndex);
    }
    
    /**
     * Converts from row and column indices for the array to the corresponding index in {@link #_real} and
     * {@link #_imag}. If no corresponding index was found then the value returned will be negative.
     * 
     * @param row
     * @param column
     * @return sparse index
     */
    int getSparseIndexForIndices(int row, int column)
    {
        int linearIndex = ArrayTransformUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column);
        
        return Arrays.binarySearch(_indices, linearIndex);
    }
    
    /**
     * Converts from row, column, and page indices for the array to the corresponding index in {@link #_real} and
     * {@link #_imag}. If no corresponding index was found then the value returned will be negative.
     * 
     * @param row
     * @param column
     * @param page
     * @return sparse index
     */
    int getSparseIndexForIndices(int row, int column, int page)
    {
        int linearIndex =
                ArrayTransformUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column, page);
        
        return Arrays.binarySearch(_indices, linearIndex);
    }
    
    /**
     * Converts from row, column, and pages indices for the array to the corresponding index in {@link #_real} and
     * {@link #_imag}. If no corresponding index was found then the value returned will be negative.
     * 
     * @param row
     * @param column
     * @param pages
     * @return sparse index
     */
    int getSparseIndexForIndices(int row, int column, int[] pages)
    {
        int linearIndex =
                ArrayTransformUtils.checkedMultidimensionalIndicesToLinearIndex(_dimensions, row, column, pages);
        
        return Arrays.binarySearch(_indices, linearIndex);
    }
    
    @Override
    public boolean equals(Object obj)
    {   
        boolean equal = false;
        
        //Same object
        if(this == obj)
        {
            equal = true;
        }
        //Same class
        else if(obj != null && this.getClass().equals(obj.getClass()))
        {
            SparseArray other = (SparseArray) obj;
            
            //If the two instances are equal their hashcodes must be equal (but not the converse)
            if(this.hashCode() == other.hashCode())
            {
                //Check equality of the elements of the arrays in expected increasing order of computational complexity
                
                //Same base components
                if(_baseComponentType.equals(other._baseComponentType))
                {
                    //Both real values, or both complex values
                    if((this.isReal() && other.isReal()) || (!this.isReal() && !other.isReal()))
                    {
                        //Same dimensions
                        if(Arrays.equals(_dimensions, other._dimensions))
                        {
                            //Same indices
                            if(Arrays.equals(_indices, other._indices))
                            {
                                //Finally, compare the inner arrays
                                equal = DynamicArrays.equals(_real, other._real) &&
                                        DynamicArrays.equals(_imag, other._imag);
                            }
                        }
                    }
                }
            }
        }
        
        return equal;
    }
    
    @Override
    public int hashCode()
    {
        if(_hashCode == null)
        {
            int hashCode = 7;
            
            hashCode = 97 * hashCode + _baseComponentType.hashCode();
            hashCode = 97 * hashCode + Arrays.hashCode(_indices);
            hashCode = 97 * hashCode + DynamicArrays.hashCode(_real);
            hashCode = 97 * hashCode + DynamicArrays.hashCode(_imag);
            hashCode = 97 * hashCode + Arrays.hashCode(_dimensions);

            _hashCode = hashCode;
        }
        
        return _hashCode;
    }
}