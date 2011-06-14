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
import java.util.HashMap;
import java.util.Map;

/**
 * A matrix of doubles from MATLAB. Supports both the real and imaginary components of the matrix.
 * <br><br>
 * Each instance knows the number of dimensions it represents and can create the corresponding multidimensional Java
 * array. In order to do this in a type safe manner the methods
 * {@link #getRealArray(matlabcontrol.extensions.MatlabMatrix.DoubleArrayType) getRealArray(...)} and
 * {@link #getImaginaryArray(matlabcontrol.extensions.MatlabMatrix.DoubleArrayType) getImaginaryArray(...)} exist. There
 * are static instances of {@link DoubleArrayType} for dimensions 2 through 9, and one may be created for a
 * {@code double} array of any dimension. For additional convenience, you can use methods {@link #getRealArray2D()},
 * {@link #getRealArray3D()}, and {@link #getRealArray4D()} (and their corresponding imaginary counterparts). All of
 * these methods will throw a {@link MatrixDimensionException} if the matrix is not actually of that dimension. While
 * these arrays will match dimension and lengths, please note that MATLAB matrices are 1-indexed while Java arrays are
 * 0-indexed. For instance in MATLAB indexing into {@code matrix} would be {@code matrix(3,4,7,2)} while in Java it
 * would be {@code matrix[2][3][6][1]}.
 * <br><br>
 * Matrices in MATLAB are stored in a linear manner. The number and lengths of the dimensions are stored separately from
 * the real and imaginary value entries. Each dimension has a fixed length. (MATLAB's array implementation is known as
 * a dope vector.)
 * <br><br>
 * Java has no built-in matrix data type, but supports arrays of {@code double}s. To support multiple dimensions,
 * Java allows for creating arrays of any data type, including arrays. (Java's array implementation is known as an
 * Iliffe vector.) A two dimensional array of {@code double}s, {@code double[][]}, is just an array of {@code double[]}.
 * A result of this is that each {@code double[]} can have a different length. When not all inner arrays for a given
 * dimension have the same length, then the array is known as as a jagged array (also known as a ragged array).
 * <br><br>
 * When a matrix is retrieved from MATLAB the resulting Java array is never jagged. When a {@code MatlabMatrix} is
 * constructed from Java arrays, the arrays provided may be jagged; see
 * {@link #MatlabMatrix(matlabcontrol.extensions.MatlabMatrix.DoubleArrayType, java.lang.Object, java.lang.Object) the
 * constructor} for
 * details.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabMatrix
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
     * The lengths in each dimension of the array.
     */
    private final int[] _lengths;
    
    /**
     * The type of array.
     */
    private final DoubleArrayType<?> _arrayType;
    
    MatlabMatrix(double[] real, double[] imaginary, int[] lengths)
    {   
        _realValues = real;
        _imaginaryValues = imaginary;
        _lengths = lengths;
        _arrayType = DoubleArrayType.get(lengths.length);
    }
       
    //Constructing from a multidimensional array
    public <T> MatlabMatrix(DoubleArrayType<T> type, T real, T imaginary)
    {
        throw new RuntimeException("Not yet implemented");
    }
    
    /**
     * Gets the real value at {@code linearIndex} treating the matrix as the underlying one dimensional matrix. This
     * is equivalent to indexing into a MATLAB matrix with just one subscript.
     * 
     * @param linearIndex
     * @return 
     */
    public double getRealValue(int linearIndex)
    {
        return _realValues[linearIndex];
    }
    
    /**
     * Gets the imaginary value at {@code linearIndex} treating the matrix as the underlying one dimensional matrix.
     * This is equivalent to indexing into a MATLAB matrix with just one subscript.
     * 
     * @param linearIndex
     * @return 
     */
    public double getImaginaryValue(int linearIndex)
    {
        return _imaginaryValues[linearIndex];
    }
    
    /**
     * Gets the real value at the specified {@code indices}. The amount of indices provided must be the number of
     * dimensions in the matrix.
     * 
     * @param indices
     * @return real value at {@code indices}
     * @throws MatrixDimensionException if number of indices is not {@code 1} or the number of dimensions
     */
    public double getRealValue(int... indices) throws MatrixDimensionException
    {
        return this.getValue(_realValues, indices);
    }
    
    /**
     * Gets the imaginary value at the specified {@code indices}. The amount of indices provided must be the number of
     * dimensions in the matrix.
     * 
     * @param indices
     * @return imaginary value at {@code indices}
     * @throws MatrixDimensionException if number of indices is not {@code 1} or the number of dimensions
     */
    public double getImaginaryValue(int... indices) throws MatrixDimensionException
    {
        return this.getValue(_imaginaryValues, indices);
    }
    
    private double getValue(double[] values, int... indices) throws MatrixDimensionException
    {
        double value;
        if(indices.length == this.getDimensions())
        {
            value = values[multidimensionalIndicesToLinearIndex(_lengths, indices)];
        }
        else
        {
            throw new MatrixDimensionException("number of indices provided [" + indices.length + "] does not match "
                    + " the number of dimensions [" + this.getDimensions() + "]");
        }
        
        return value;
    }
    
    /**
     * The number of dimensions this matrix has.
     * 
     * @return 
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
     * @return 
     */
    public int[] getLengths()
    {
        int[] lengthsCopy = new int[_lengths.length];
        System.arraycopy(_lengths, 0, lengthsCopy, 0, _lengths.length);
        
        return lengthsCopy;
    }
    
    private <T> T getAsJavaArray(DoubleArrayType<T> type, double[] values) throws MatrixDimensionException
    {
        if(type._numDimensions != _arrayType._numDimensions)
        {
            throw new MatrixDimensionException("matrix has " + _arrayType._numDimensions + " dimensions, it cannot be "
                    + "retrieved as a " + type._numDimensions + " dimensional Java array");
        }
        
        return multidimensionalize(values, type._arrayClass, _lengths);
    }
    
    /**
     * Returns a {@code double} array of type {@code T} that holds the real values from the MATLAB array.
     * 
     * @param <T>
     * @param type
     * @return 
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not of the dimension
     * specified by {@code type}
     */
    public <T> T getRealArray(DoubleArrayType<T> type) throws MatrixDimensionException
    {
        return getAsJavaArray(type, _realValues);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_2)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a two dimensional
     * matrix
     */
    public double[][] getRealArray2D() throws MatrixDimensionException
    {
        return this.getRealArray(DoubleArrayType.DIM_2);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_3)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a three dimensional
     * matrix
     */
    public double[][][] getRealArray3D() throws MatrixDimensionException
    {
        return this.getRealArray(DoubleArrayType.DIM_3);
    }
    
    /**
     * Equivalent to {@code getRealArray(DoubleArrayType.DIM_4)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a four dimensional
     * matrix
     */
    public double[][][][] getRealArray4D() throws MatrixDimensionException
    {
        return this.getRealArray(DoubleArrayType.DIM_4);
    }
    
    /**
     * Returns a {@code double} array of type {@code T} that holds the imaginary values from the MATLAB array.
     * 
     * @param <T>
     * @param type
     * @return 
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not of the dimension
     * specified by {@code type}
     */
    public <T> T getImaginaryArray(DoubleArrayType<T> type) throws MatrixDimensionException
    {
        return getAsJavaArray(type, _imaginaryValues);
    }
        
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_2)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a two dimensional
     * matrix
     */
    public double[][] getImaginaryArray2D() throws MatrixDimensionException
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_2);
    }
            
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_3)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a three dimensional
     * matrix
     */
    public double[][][] getImaginaryArray3D() throws MatrixDimensionException
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_3);
    }
            
    /**
     * Equivalent to {@code getImaginaryArray(DoubleArrayType.DIM_4)}.
     * 
     * @return
     * @throws matlabcontrol.extensions.MatlabMatrix.MatrixDimensionException if the matrix is not a four dimensional
     * matrix
     */
    public double[][][][] getImaginaryArray4D() throws MatrixDimensionException
    {
        return this.getImaginaryArray(DoubleArrayType.DIM_4);
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
            int pageArea = 1; //Need better variable name
            for(int dim = 2; dim < lengths.length; dim++)
            {
                indices[dim] = (pageNumber / pageArea) % lengths[dim];
                pageArea *= lengths[dim];
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
     * @throws IllegalArgumentException 
     */
    private static int multidimensionalIndicesToLinearIndex(int[] lengths, int[] indices) throws IllegalArgumentException
    {
        if(lengths.length != indices.length)
        {
            throw new IllegalArgumentException("There must be an equal number of lengths and indices");
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
    
    /*
    private static <T> T multidimensionalize(double[] linearArray, Class<T> outputArrayType, int[] lengths,
            boolean validate) throws MatlabProcessingException
    {
        //Validate if requested
        if(validate)
        {
            int _numDimensions = getNumberOfDimensions(outputArrayType);
            //Check the outputArrayType is actually a type of array
            if(_numDimensions == 0)
            {
                throw new MatlabProcessingException(outputArrayType + " is not an array type");
            }
            //Check the number of dimensions of the array matches the number of lengths provided
            else if(_numDimensions != lengths.length)
            {
                throw new MatlabProcessingException("array dimensions [" + _numDimensions + "] do not match number of provided "
                        + "lengths [" + lengths.length + "]");
            }
            //Check the outputArrayType will hold doubles
            else if(!isDoubleArrayType(outputArrayType))
            {
                throw new MatlabProcessingException(outputArrayType + " is not a double array");
            }
            
            //Check the size of the linear array and the size of the output array will match
            int outputArraySize = 1;
            for(int length : lengths)
            {
                outputArraySize *= length;
            }
            
            if(outputArraySize != linearArray.length)
            {
                throw new MatlabProcessingException("linear array size [" + linearArray.length + "] does not match "
                        + "size computed from lengths [" + outputArraySize + "]");
            }
        }
        
        return multidimensionalize_internal(linearArray, outputArrayType, lengths, 0, new int[0]);
    }
    */
    
    private static <T> T multidimensionalize(double[] linearArray, Class<T> outputArrayType, int[] lengths)
    {
        return multidimensionalize_internal(linearArray, outputArrayType, lengths, 0, new int[0]);
    }

    
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
     * Represents attempting to retrieve or manipulate a MATLAB matrix as the wrong dimension.
     */
    public static class MatrixDimensionException extends RuntimeException
    {
        MatrixDimensionException(String msg)
        {
            super(msg);
        }
    }
    
    
    /**
     * An array type of any dimension that holds {@code double}s. Instances for dimensions 2 through 9 are available as
     * public static fields.
     * 
     * @param <T> an array of 1 or more dimensions which holds {@code double}s
     */
    public static final class DoubleArrayType<T>
    {
        /**
         * Representation of {@code double[][]} class.
         */
        public static final DoubleArrayType<double[][]> DIM_2 = get(double[][].class);
        
        /**
         * Representation of {@code double[][][]} class.
         */
        public static final DoubleArrayType<double[][][]> DIM_3 = get(double[][][].class);
        
        /**
         * Representation of {@code double[][][][]} class.
         */
        public static final DoubleArrayType<double[][][][]> DIM_4 = get(double[][][][].class);
        
                
        /**
         * Representation of {@code double[][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][]> DIM_5 = get(double[][][][][].class);
        
                
        /**
         * Representation of {@code double[][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][]> DIM_6 = get(double[][][][][][].class);
                        
        /**
         * Representation of {@code double[][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][]> DIM_7 = get(double[][][][][][][].class);
        
        /**
         * Representation of {@code double[][][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][][]> DIM_8 = get(double[][][][][][][][].class);
        
        /**
         * Representation of {@code double[][][][][][][][][]} class.
         */
        public static final DoubleArrayType<double[][][][][][][][][]> DIM_9 = get(double[][][][][][][][][].class);
        
        /**
         * Caches loaded {@code DoubleArrayType}s.
         */
        private static final Map<Class, DoubleArrayType> CLASS_TO_ARRAY_TYPE = new HashMap<Class, DoubleArrayType>();
        
        /**
         * The array class represented.
         */
        private final Class<T> _arrayClass;
        
        /**
         * The number of dimensions of the array type.
         */
        private final int _numDimensions;
        
        private DoubleArrayType(Class<T> arrayClass) throws IllegalArgumentException
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
         * must be an array of 1 or more dimensions that holds {@code double}s. This intended for getting array types
         * in excess of 9 dimensions, as dimensions 2 through 9 are represented by constants {@code DIM_2 ... DIM_9}.
         * <br><br>
         * Example usage:<br>
         * {@code DoubleArrayType<double[][][]> type3D = DoubleArrayType.get(double[][][].class)}
         * 
         * @param <T>
         * @param arrayType
         * @return
         * @throws IllegalArgumentException if the type is not an array holding {@code double}s
         */
        public static <T> DoubleArrayType<T> get(Class<T> arrayType) throws IllegalArgumentException
        {
            if(!CLASS_TO_ARRAY_TYPE.containsKey(arrayType))
            {
                DoubleArrayType<T> type = new DoubleArrayType<T>(arrayType);
                CLASS_TO_ARRAY_TYPE.put(arrayType, type);
            }
            
            return CLASS_TO_ARRAY_TYPE.get(arrayType);
        }
        
        static DoubleArrayType<?> get(int dimensions)
        {
            DoubleArrayType<?> type;
            
            //Construct the name of the class
            String className = "";
            for(int i = 0; i < dimensions; i++)
            {
                className += "[";
            }
            className += "D";

            //Retrieve the class, and then get the corresponding DoubleArrayType
            try
            {
                type = get(Class.forName(className));
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
         * The type of array. The array holds {@code double}s, and may be of any dimension.
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
         * Returns the number of dimensions the array type has. If {@code type} is not a type of array then {@code 0} will
         * be returned.
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
    }
}