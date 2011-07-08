package matlabcontrol.extensions;

/*
 * Copyright (c) 2011, Joshua Kaplan
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Acts as a MATLAB array of doubles. MATLAB arrays of any numeric type may be represented by a
 * {@code MatlabNumericArray}, but precision may be lost in the process. Dimensions of 2 and greater are supported. (No
 * array in MATLAB has a dimension less than 2.) This representation is a copy of the MATLAB data, not a live view.
 * Retrieving large arrays from MATLAB can result in a {@link OutOfMemoryError}; if this occurs you may want to either
 * retrieve only part of the array from MATLAB or increase your Java Virtual Machine's heap size.
 * <br><br>
 * <b>Note:</b> Sparse arrays are not supported. Attempts to retrieve a sparse may not throw an exception, but the
 * result will not be valid.
 * <br><br>
 * Both the real and imaginary components of a MATLAB array are supported. If the array has no imaginary values then
 * attempts to access these values will result in a {@link IllegalStateException} being thrown.
 * <br><br>
 * Arrays in MATLAB are stored in a linear manner. The number and lengths of the dimensions are stored separately from
 * the real and imaginary value entries. Each dimension has a fixed length. (MATLAB's array implementation is known as
 * a dope vector.)
 * <br><br>
 * Java has no multidimensional array type. To support multiple dimensions, Java allows for creating arrays of any data
 * type, including arrays. (Java's array implementation is known as an Iliffe vector.) A two dimensional array of
 * {@code double}s, {@code double[][]}, is just an array of {@code double[]}. A result of this is that each
 * {@code double[]} can have a different length. When not all inner arrays for a given dimension have the same length,
 * then the array is known as as a jagged array (also known as a ragged array).
 * <br><br>
 * When an array is retrieved from MATLAB the resulting Java array is never jagged. When a {@code MatlabNumericArray} is
 * constructed from Java arrays, the arrays provided may be jagged; see the
 * {@link #MatlabNumericArray(DoubleArrayType, java.lang.Object, java.lang.Object) main constructor} for details.
 * <br><br> 
 * Each instance knows the number of dimensions it represents and can create the corresponding multidimensional Java
 * array. In order to do this in a type safe manner the methods {@link #getRealArray(DoubleArrayType) getRealArray(...)}
 * and {@link #getImaginaryArray(DoubleArrayType) getImaginaryArray(...)} exist. Convenience methods exist to easily
 * retrieve the arrays as two, three, and four dimensional arrays. All of these methods will throw a
 * {@link ArrayDimensionException} if the array is not actually of that dimension. It is also possible to retrieve
 * values from the array without converting it to the corresponding multidimensional Java array. This can be done either
 * by using the index into the underlying linear MATLAB array, or by using the multidimensional indices. Retrieving
 * values in this manner does not require the computation and memory necessary to create the multidimensional Java
 * array.
 * <br><br>
 * While this class mimics the dimension and lengths of a MATLAB array, it uses Java's 0-index convention instead of
 * MATLAB's 1-index convention. For instance in MATLAB if an array were indexed into as {@code array(3,4,7,2)}, then in
 * Java to retrieve the same entry the indexing would be performed as {@code array[2][3][6][1]}.
 * <br><br>
 * Once constructed, this class is unconditionally thread-safe. If the data provided to a constructor is modified while
 * construction is occurring, problems may occur.
 * 
 * @see TypeConverter#setNumericArray(java.lang.String, matlabcontrol.extensions.MatlabNumericArray)
 * @see TypeConverter#getNumericArray(java.lang.String) 
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabNumericArray
{   
    /**
     * Linear array of real values.
     */
    private final double[] _realValues;
    
    /**
     * Linear array of imaginary values.
     */
    private final double[] _imaginaryValues;
    
    /**
     * The lengths for each dimension of the array.
     */
    private final int[] _lengths;
    
    /**
     * The type of double array.
     */
    private final DoubleArrayType<?> _arrayType;
    
    /**
     * If the array was retrieved from MATLAB.
     */
    private final boolean _fromMatlab;
    
    /**
     * If the array is composed entirely of real values. If this is {@code true} then {@link #_imaginaryValues} will be
     * {@code null}.
     */
    private final boolean _isReal;
    
    /**
     * Constructs an array from data retrieved from MATLAB.
     * 
     * @param real
     * @param imaginary
     * @param lengths 
     */
    MatlabNumericArray(double[] real, double[] imaginary, int[] lengths)
    {   
        _fromMatlab = true;
        
        _realValues = real;
        _imaginaryValues = imaginary;
        _isReal = (imaginary == null);
        
        _lengths = lengths;
        _arrayType = DoubleArrayType.getInstance(lengths.length);
    }
    
    /**
     * Constructs a numeric array from Java arrays that can be transferred to MATLAB. The {@code imaginary} array
     * may be {@code null}, if so then this array will be real. References to the arrays passed in are not kept, and
     * modifying the array data after this class has been constructed will have no effect. If the data is modified
     * concurrently with this class's construction, problems may arise.
     * <br><br>
     * The arrays may be jagged; however, MATLAB does not support jagged arrays and therefore the arrays will be treated
     * as if they had uniform length for each dimension. For each dimension the maximum length is determined. If both
     * the {@code real} and {@code imaginary} arrays are provided then the maximum length per dimension is determined
     * across both arrays. For parts of the array that have a length less than the maximum length, {@code 0} will be
     * used.
     * 
     * @param <T>
     * @param type may not be {@code null}
     * @param real may not be {@code null}
     * @param imaginary may be {@code null}, if {@code null} then this array will be real
     * @throws NullPointerException
     */
    public <T> MatlabNumericArray(DoubleArrayType<T> type, T real, T imaginary)
    {        
        //Validate input
        if(type == null)
        {
            throw new NullPointerException("The type of the arrays may not be null.");
        }        
        if(real == null)
        {
            throw new NullPointerException("Real array may not be null.");
        }
        
        _fromMatlab = false;
        _isReal = (imaginary == null);
        
        //Store type
        _arrayType = type;
        
        //Determine lengths
        _lengths = new int[type.getDimensions()];
        int[] realLengths = computeBoundingLengths(real);
        for(int i = 0; i < realLengths.length; i++)
        {
            _lengths[i] = Math.max(_lengths[i], realLengths[i]);
        }
        if(imaginary != null)
        {
            int[] imaginaryLengths = computeBoundingLengths(imaginary);
            for(int i = 0; i < imaginaryLengths.length; i++)
            {
                _lengths[i] = Math.max(_lengths[i], imaginaryLengths[i]);
            }
        }
        
        //Linearize arrays
        _realValues = linearize(real, _lengths);
        if(imaginary != null)
        {
            _imaginaryValues = linearize(imaginary, _lengths);
        }
        else
        {   
            _imaginaryValues = null;
        }
    }
    
    /**
     * Convenience constructor, equivalent to {@code new MatlabNumericArray(DoubleArrayType.DIM_2, real, imaginary)}.
     * 
     * @param real
     * @param imaginary 
     * @throws NullPointerException
     */
    public MatlabNumericArray(double[][] real, double[][] imaginary)
    {
        this(DoubleArrayType.DIM_2, real, imaginary);
    }
    
    /**
     * Convenience constructor, equivalent to {@code new MatlabNumericArray(DoubleArrayType.DIM_3, real, imaginary)}.
     * 
     * @param real
     * @param imaginary 
     * @throws NullPointerException
     */
    public MatlabNumericArray(double[][][] real, double[][][] imaginary)
    {
        this(DoubleArrayType.DIM_3, real, imaginary);
    }
    
    /**
     * Convenience constructor, equivalent to {@code new MatlabNumericArray(DoubleArrayType.DIM_4, real, imaginary)}.
     * 
     * @param real
     * @param imaginary 
     * @throws NullPointerException
     */
    public MatlabNumericArray(double[][][][] real, double[][][][] imaginary)
    {
        this(DoubleArrayType.DIM_4, real, imaginary);
    }
    
    /**
     * Computes the total size of an array with lengths specified by {@code lengths}.
     * 
     * @param lengths
     * @return 
     */
    private static int getTotalSize(int[] lengths)
    {
        int size = 0;
        
        for(int length : lengths)
        {
            if(size == 0)
            {
                size = length;
            }
            else if(length != 0)
            {
                size *= length;
            }
        }
        
        return size;
    }
    
    /**
     * Gets the real value at {@code linearIndex} treating this array as the underlying one dimensional array. This
     * is equivalent to indexing into a MATLAB array with just one subscript.
     * 
     * @param linearIndex
     * @return real value at {@code linearIndex}
     * @throws ArrayIndexOutOfBoundsException
     */
    public double getRealValue(int linearIndex)
    {
        return _realValues[linearIndex];
    }
    
    /**
     * Gets the imaginary value at {@code linearIndex} treating this array as the underlying one dimensional array.
     * This is equivalent to indexing into a MATLAB array with just one subscript.
     * 
     * @param linearIndex
     * @return imaginary value at {@code linearIndex}
     * @throws ArrayIndexOutOfBoundsException
     * @throws IllegalStateException if the array is real
     */
    public double getImaginaryValue(int linearIndex)
    {
        if(_isReal)
        {
            throw new IllegalStateException("array is real");
        }
        
        return _imaginaryValues[linearIndex];
    }
    
    /**
     * Gets the real value at the specified {@code indices}. The amount of indices provided must be the number of
     * dimensions in the array.
     * 
     * @param indices
     * @return real value at {@code indices}
     * @throws ArrayDimensionException if number of indices is not the number of dimensions
     * @throws IndexOutOfBoundsException if the indices are out of bound
     */
    public double getRealValue(int... indices)
    {
        return this.getValue(_realValues, indices);
    }
    
    /**
     * Gets the imaginary value at the specified {@code indices}. The amount of indices provided must be the number of
     * dimensions in the array.
     * 
     * @param indices
     * @return imaginary value at {@code indices}
     * @throws ArrayDimensionException if number of indices is not the number of dimensions
     * @throws IndexOutOfBoundsException if the indices are out of bound
     * @throws IllegalStateException if the array is real
     */
    public double getImaginaryValue(int... indices)
    {
        if(_isReal)
        {
            throw new IllegalStateException("array is real");
        }
        
        return this.getValue(_imaginaryValues, indices);
    }
    
    private double getValue(double[] values, int... indices) throws ArrayDimensionException, ArrayIndexOutOfBoundsException
    {
        double value;
        if(indices.length == this.getDimensions())
        {
            //Check the indices are in bounds
            for(int i = 0; i < indices.length; i++)
            {
                if(indices[i] >= _lengths[i])
                {
                    throw new IndexOutOfBoundsException("[" + indices[i] + "] is out of bounds for dimension " +
                            i + " where the length is " + _lengths[i]);
                }
            }
            
            
            value = values[multidimensionalIndicesToLinearIndex(_lengths, indices)];
        }
        else
        {
            throw new ArrayDimensionException(_arrayType.getDimensions(), indices.length);
        }
        
        return value;
    }
    
    /**
     * The number of dimensions this array has.
     * 
     * @return number of dimensions
     */
    public int getDimensions()
    {
        return _arrayType.getDimensions();
    }
    
    /**
     * Returns the lengths of each dimension with respect to their index. In MATLAB the first dimension (0 index in the
     * returned integer array) is the row length. The second dimension is the column length. The third dimension and
     * beyond are pages. The length of the returned array will be the number of dimensions returned by
     * {@link #getDimensions()}.
     * 
     * @return lengths
     */
    public int[] getLengths()
    {
        int[] lengthsCopy = new int[_lengths.length];
        System.arraycopy(_lengths, 0, lengthsCopy, 0, _lengths.length);
        
        return lengthsCopy;
    }
    
    /**
     * The length of the underlying linear array.
     * 
     * @return length
     */
    public int getLength()
    {
        return _realValues.length;
    }
    
    private <T> T getAsJavaArray(DoubleArrayType<T> type, double[] values)
    {
        if(type.getDimensions() != _arrayType.getDimensions())
        {
            throw new ArrayDimensionException(_arrayType.getDimensions(), type.getDimensions());
        }
        
        return multidimensionalize(values, type._arrayClass, _lengths);
    }
    
    /**
     * Returns a {@code double} array of type {@code T} that holds the real values from the MATLAB array.
     * 
     * @param <T>
     * @param type
     * @return 
     * @throws ArrayDimensionException if the array is not of the dimension specified by {@code type}
     */
    public <T> T getRealArray(DoubleArrayType<T> type)
    {
        return getAsJavaArray(type, _realValues);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_2)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a two dimensional array
     */
    public double[][] getRealArray2D()
    {
        return this.getRealArray(DoubleArrayType.DIM_2);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_3)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a three dimensional array
     */
    public double[][][] getRealArray3D() 
    {
        return this.getRealArray(DoubleArrayType.DIM_3);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_4)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a four dimensional array
     */
    public double[][][][] getRealArray4D()
    {
        return this.getRealArray(DoubleArrayType.DIM_4);
    }
    
    /**
     * Returns a {@code double} array of type {@code T} that holds the imaginary values from the MATLAB array.
     * 
     * @param <T>
     * @param type
     * @return 
     * @throws ArrayDimensionException if the array is not of the dimension specified by {@code type}
     * @throws IllegalStateException if the array is real
     */
    public <T> T getImaginaryArray(DoubleArrayType<T> type)
    {
        if(_isReal)
        {
            throw new IllegalStateException("array is real");
        }
        
        return getAsJavaArray(type, _imaginaryValues);
    }
        
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_2)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a two dimensional array
     * @throws IllegalStateException if the array is real
     */
    public double[][] getImaginaryArray2D()
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_2);
    }
            
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_3)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a three dimensional array
     * @throws IllegalStateException if the array is real
     */
    public double[][][] getImaginaryArray3D()
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_3);
    }
            
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_4)}.
     * 
     * @return
     * @throws ArrayDimensionException if the array is not a four dimensional array
     * @throws IllegalStateException if the array is real
     */
    public double[][][][] getImaginaryArray4D()
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_4);
    }
    
    /**
     * Returns {@code true} if the array has no imaginary values, {@code false} otherwise. Equivalent to the MATLAB
     * {@code isreal} function.
     * 
     * @return 
     */
    public boolean isReal()
    {
        return _isReal;
    }
    
    /**
     * Returns the underlying linear array of real values. Returns the actual array, not a copy.
     * 
     * @return 
     */
    double[] getRealLinearArray()
    {
        return _realValues;
    }
    
    /**
     * Returns the underlying linear array of imaginary values. Returns the actual array, not a copy.
     * 
     * @return 
     */
    double[] getImaginaryLinearArray()
    {
        return _imaginaryValues;
    }
    
    /**
     * Returns a brief description of this array. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass() +
                " dimensions=" + this.getDimensions() + "," +
                " linearLength=" + this.getLength() + "," +
                " lengths=" + Arrays.toString(_lengths) + "," +
                " fromMATLAB=" + _fromMatlab + "]";
    }
    
    /**
     * Linear index to multidimensional indices. Similar to MATLAB's (@code ind2sub} function.
     * 
     * @param lengths the lengths of the array in each dimension
     * @param linearIndex
     * @return 
     */
    private static int[] linearIndexToMultidimensionalIndices(int[] lengths, int linearIndex)
    {
        int[] indices = new int[lengths.length];
        
        if(lengths.length == 1)
        {
            indices[0] = linearIndex;
        }
        else
        {
            int pageSize = lengths[0] * lengths[1];
            int pageNumber = linearIndex / pageSize;

            //Row and column
            int indexInPage = linearIndex % pageSize;
            indices[0] = indexInPage % lengths[0];
            indices[1] = indexInPage / lengths[0];

            //3rd dimension and above
            int accumSize = 1;
            for(int dim = 2; dim < lengths.length; dim++)
            {
                indices[dim] = (pageNumber / accumSize) % lengths[dim];
                accumSize *= lengths[dim];
            }
        }
        
        return indices;
    }
        
    /**
     * Multidimensional indices to linear index. Similar to MATLAB's (@code sub2ind} function.
     * 
     * @param lengths the lengths of the array in each dimension
     * @param indices
     * @return
     * @throws IllegalArgumentException thrown if the length of {@code lengths} and {@code indices} are not the same
     */
    private static int multidimensionalIndicesToLinearIndex(int[] lengths, int[] indices)
    {
        if(lengths.length != indices.length)
        {
            throw new IllegalArgumentException("There must be an equal number of lengths [" + lengths.length + "] "
                    + "and indices [" + indices.length + "]");
        }
        
        int linearIndex = 0;
        
        int accumSize = 1;
        for(int i = 0; i < lengths.length; i++)
        {   
            linearIndex += accumSize * indices[i];
            accumSize *= lengths[i];
        }
        
        return linearIndex;
    }
    
    /**
     * Creates a {@code double} array of type {@code T} with the length of each dimension specified by {@code lengths}.
     * As the array is created it is populated with the values of {@code linearArray}.
     * 
     * @param <T>
     * @param linearArray
     * @param outputArrayType
     * @param lengths
     * @return 
     */
    private static <T> T multidimensionalize(double[] linearArray, Class<T> outputArrayType, int[] lengths)
    {
        return multidimensionalize_internal(linearArray, outputArrayType, lengths, 0, new int[0]);
    }

    /**
     * The real logic of the {@link #multidimensionalize(double[], java.lang.Class, int[])} method. This method
     * recurs on itself, hence the need for the extra parameters. This method does not store state in any external
     * variables.
     * 
     * @param <T>
     * @param linearArray
     * @param outputArrayType
     * @param lengths
     * @param indexIntoLengths should be {@code 0} initially
     * @param currIndices should be an empty integer array initially
     * @return 
     */
    private static <T> T multidimensionalize_internal(double[] linearArray, Class<T> outputArrayType, int[] lengths,
            int indexIntoLengths, int[] currIndices)
    {
        Class<?> arrayType = outputArrayType.getComponentType();
        int arrayLength = lengths[indexIntoLengths];
        T array = (T) Array.newInstance(arrayType, arrayLength);
        
        //If what was created holds an array, then fill it
        if(arrayType.isArray())
        {
            //If the array that was created holds the double array, double[]
            if(arrayType.equals(double[].class))
            {
                //The index in the multidimensional array being created
                int[] primitiveArrayIndices = new int[currIndices.length + 2];
                System.arraycopy(currIndices, 0, primitiveArrayIndices, 0, currIndices.length);
                
                //Iterate over the created array, placing a double[] array in each index
                for(int i = 0; i < arrayLength; i++)
                {
                    primitiveArrayIndices[primitiveArrayIndices.length - 2] = i;
                
                    //Create the primitive array
                    double[] primitiveArray = new double[lengths[lengths.length - 1]];
                    
                    //Fill the primitive array with values from the linear array
                    for(int j = 0; j < primitiveArray.length; j++)
                    {                    
                        primitiveArrayIndices[primitiveArrayIndices.length - 1] = j;
                        int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primitiveArrayIndices);
                        primitiveArray[j] = linearArray[linearIndex];
                    }
                    
                    //Place primitive array into the enclosing array
                    Array.set(array, i, primitiveArray);
                }
            }
            else
            {
                //Iterate over the created array, placing an array in each index (using recursion)
                for(int i = 0; i < arrayLength; i++)
                {   
                    int[] nextIndices = new int[currIndices.length + 1];
                    System.arraycopy(currIndices, 0, nextIndices, 0, currIndices.length);
                    nextIndices[nextIndices.length - 1] = i;
                    
                    Object innerArray = multidimensionalize_internal(linearArray, arrayType, lengths,
                            indexIntoLengths + 1, nextIndices);
                    Array.set(array, i, innerArray);
                }
            }
        }
        //If a double[] was just created, then the original request was to create a one dimensional array
        else
        {
            System.arraycopy(linearArray, 0, array, 0, arrayLength);
        }
        
        return array;
    }
    
    /**
     * Determines the maximum length for each dimension of the array.
     * 
     * @param array
     * @return 
     */
    private static int[] computeBoundingLengths(Object array)
    {   
        DoubleArrayType<?> type = DoubleArrayType.getInstanceUnsafe(array.getClass());
        int[] maxLengths = new int[type.getDimensions()];

        //The length of this array
        int arrayLength = Array.getLength(array);
        maxLengths[0] = arrayLength;

        //If the array holds arrays as its entries
        if(!array.getClass().getComponentType().equals(double.class))
        {
            //For each entry in the array
            for(int i = 0; i < arrayLength; i++)
            {   
                //childLengths' information will be one index ahead of maxLengths
                int[] childLengths = computeBoundingLengths(Array.get(array, i));
                for(int j = 0; j < childLengths.length; j++)
                {
                    maxLengths[j + 1] = Math.max(maxLengths[j + 1], childLengths[j]);
                }
            }
        }
        
        return maxLengths;
    }
    
    /**
     * Creates a linearized version of the {@code array} with maximum length in each dimension specified by
     * {@code lengths}. No verification is performed that the dimensions of the array match the length of the lengths
     * array.
     * 
     * @param array
     * @param lengths
     * @return 
     */
    private static double[] linearize(Object array, int[] lengths)
    {
        //Create linear array with equal size of the array
        double[] linearArray = new double[getTotalSize(lengths)];
        
        //Fill linearArray with values from array
        linearize_internal(linearArray, array, lengths, new int[0]);
        
        return linearArray;
    }
    
    /**
     * Performs the linearization using recursion.
     * 
     * @param linearArray array to be filled
     * @param array source array
     * @param lengths the lengths of the array for the initial array supplied before any recursion
     * @param currIndices must initially be an empty integer array
     */
    private static void linearize_internal(double[] linearArray, Object array, int[] lengths, int[] currIndices)
    {
        //Base case
        if(array.getClass().equals(double[].class))
        {
            int[] doubleArrayIndices = new int[currIndices.length + 1];
            System.arraycopy(currIndices, 0, doubleArrayIndices, 0, currIndices.length);
            
            //Fill linear array with contents of the double[] array
            double[] doubleArray = (double[]) array;
            for(int i = 0; i < doubleArray.length; i++)
            {
                doubleArrayIndices[doubleArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, doubleArrayIndices);
                linearArray[linearIndex] = doubleArray[i];
            }
        }
        else
        {
            int arrayLength = Array.getLength(array);
            for(int i = 0; i < arrayLength; i++)
            {
                int[] nextIndices = new int[currIndices.length + 1];
                System.arraycopy(currIndices, 0, nextIndices, 0, currIndices.length);
                nextIndices[nextIndices.length - 1] = i;
                
                linearize_internal(linearArray, Array.get(array, i), lengths, nextIndices);
            }
        }
    }
    
    /**
     * Represents attempting to retrieve or manipulate a MATLAB array as the wrong dimension.
     */
    public static class ArrayDimensionException extends RuntimeException
    {
        private static final long serialVersionUID = 0xC400L;
    
        private final int _actualNumberOfDimensions;
        private final int _usedAsNumberOfDimensions; 
        
        ArrayDimensionException(int actualNumDim, int usedAsNumDim)
        {
            super("Array has " + actualNumDim + " dimension(s), it cannot be used as if it had " + usedAsNumDim +
                    " dimension(s).");
            
            _actualNumberOfDimensions = actualNumDim;
            _usedAsNumberOfDimensions = usedAsNumDim;
        }
        
        /**
         * The actual number of dimensions the array has.
         * 
         * @return 
         */
        public int getActualNumberOfDimensions()
        {
            return _actualNumberOfDimensions;
        }
        
        /**
         * The number of dimensions that were used when interacting with the array.
         * 
         * @return 
         */
        public int getUsedNumberOfDimensions()
        {
            return _usedAsNumberOfDimensions;
        }
    }
    
    /**
     * An array type of dimension 2 or greater which holds {@code double}s. Instances for dimensions 2 through 9 are
     * available as {@code public static} fields.
     * <br><br>
     * This class is unconditionally thread-safe.
     * 
     * @param <T> an array of 2 or more dimensions which holds {@code double}s
     */
    public static final class DoubleArrayType<T>
    {
        /**
         * Caches loaded {@code DoubleArrayType}s.
         */
        private static final Map<Class<?>, DoubleArrayType> CLASS_TO_ARRAY_TYPE =
                new ConcurrentHashMap<Class<?>, DoubleArrayType>();
        
        /**
         * Representation of {@code double[][]} class.
         */
        public static final DoubleArrayType<double[][]> DIM_2 = getInstance(double[][].class);
        
        /**
         * Representation of {@code double[][][]} class.
         */
        public static final DoubleArrayType<double[][][]> DIM_3 = getInstance(double[][][].class);
        
        /**
         * Representation of {@code double[][][][]} class.
         */
        public static final DoubleArrayType<double[][][][]> DIM_4 = getInstance(double[][][][].class);
                
        /**
         * Representation of {@code double[][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][]> DIM_5 = getInstance(double[][][][][].class);
                
        /**
         * Representation of {@code double[][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][]> DIM_6 = getInstance(double[][][][][][].class);
                        
        /**
         * Representation of {@code double[][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][]> DIM_7 = getInstance(double[][][][][][][].class);
        
        /**
         * Representation of {@code double[][][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][][]> DIM_8 = getInstance(double[][][][][][][][].class);
        
        /**
         * Representation of {@code double[][][][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][][][]> DIM_9 = getInstance(double[][][][][][][][][].class);
        
        /**
         * The array class represented.
         */
        private final Class<T> _arrayClass;
        
        /**
         * The number of dimensions of the array type.
         */
        private final int _numDimensions;
        
        /**
         * Constructs a representation of a multidimensional array of {@code double}s.
         * 
         * @param arrayClass
         * @throws IllegalArgumentException if the type is not an array holding {@code double}s
         */
        private DoubleArrayType(Class<T> arrayClass)
        {
            if(!isDoubleArrayType(arrayClass))
            {
                throw new IllegalArgumentException(arrayClass + " does not hold doubles");
            }
            
            _arrayClass = arrayClass;
            _numDimensions = getNumberOfDimensions(arrayClass);
        }
        
        /**
         * Gets an instance of {@code DoubleArrayType<T>} where {@code T} is the type of {@code arrayType}. {@code T}
         * must be an array of 1 or more dimensions that holds {@code double}s. This is intended for getting array types
         * in excess of 9 dimensions, as dimensions 2 through 9 are represented by constants {@code DIM_2 ... DIM_9}.
         * <br><br>
         * Contrived example usage:<br>
         * {@code DoubleArrayType<double[][][]> type3D = DoubleArrayType.getInstance(double[][][].class);}
         * 
         * @param <T>
         * @param arrayType
         * 
         * @return
         * 
         * @throws IllegalArgumentException if the type is not an array holding {@code double}s or the type is of less
         * than 2 dimensions
         */
        public static <T> DoubleArrayType<T> getInstance(Class<T> arrayType)
        {
            if(arrayType.equals(double[].class))
            {
                throw new IllegalArgumentException(arrayType + " not supported, must be 2 or more dimensions");
            }
            
            return getInstanceUnsafe(arrayType);
        }
        
        /**
         * Behaves the same as {@link #getInstance(java.lang.Class)} except that {@code double[]} is valid. This is
         * needed by some of the recursive algorithms in {@code MatlabNumericArray}.
         */
        static <T> DoubleArrayType<T> getInstanceUnsafe(Class<T> arrayType)
        {
            if(!CLASS_TO_ARRAY_TYPE.containsKey(arrayType))
            {
                DoubleArrayType<T> type = new DoubleArrayType<T>(arrayType);
                CLASS_TO_ARRAY_TYPE.put(arrayType, type);
            }
            
            return CLASS_TO_ARRAY_TYPE.get(arrayType);
        }
        
        static DoubleArrayType<?> getInstance(int dimensions)
        {
            DoubleArrayType<?> type;
            
            //Construct the name of the class
            String className = "";
            for(int i = 0; i < dimensions; i++)
            {
                className += "[";
            }
            className += "D";

            //Retrieve the class, and then getInstance the corresponding DoubleArrayType
            try
            {
                type = getInstanceUnsafe(Class.forName(className));
            }
            catch(ClassNotFoundException e)
            {
                type = null;
            }
            
            return type;
        }
        
        /**
         * The number of dimensions of the array type.
         * 
         * @return 
         */
        public int getDimensions()
        {
            return _numDimensions;
        }
        
        /**
         * The type of array. The array holds {@code double}s, and may be of any dimension 2 or greater.
         * 
         * @return 
         */
        public Class<T> getArrayClass()
        {
            return _arrayClass;
        }
        
        /**
         * If {@code type} is an array of one or more dimensions which holds doubles.
         * 
         * @param type
         * @return 
         */
        private static boolean isDoubleArrayType(Class<?> type)
        {
            boolean isType;

            if(type.isArray())
            {
                while(type.isArray())
                {
                    type = type.getComponentType();
                }

                isType = type.equals(double.class);
            }
            else
            {
                isType = false;
            }

            return isType;
        }
        
        /**
         * Returns the number of dimensions the array type has. If {@code type} is not a type of array then {@code 0}
         * will be returned.
         * 
         * @param type
         * @return 
         */
        private static int getNumberOfDimensions(Class<?> type)
        {
            int numDim = 0;
            while(type.isArray())
            {
                numDim++;
                type = type.getComponentType();
            }

            return numDim;
        }
        
        /**
         * Returns a brief description of this double array type. The exact details of this representation are
         * unspecified and are subject to change.
         * 
         * @return 
         */
        @Override
        public String toString()
        {
            return "[" + this.getClass().getName() + " class=" + _arrayClass + ", dimensions=" + _numDimensions + "]";
        }
    }
}