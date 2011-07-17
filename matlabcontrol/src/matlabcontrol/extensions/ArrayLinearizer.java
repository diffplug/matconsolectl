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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.extensions.MatlabType.MatlabTypeSerializedSetter;

/**
 *
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayLinearizer
{
    static boolean isMultidimensionalPrimitiveArray(Class<?> clazz)
    {
        boolean isMultiPrim = false;
        
        //If multidimensional array
        if(clazz.isArray() && clazz.getComponentType().isArray())
        {
            //Get base type of array
            while(clazz.isArray())
            {
                clazz = clazz.getComponentType();
            }
            
            isMultiPrim = clazz.isPrimitive();
        }
        
        return isMultiPrim;
    }
    
    static MatlabTypeSerializedSetter getSerializedSetter(Object array)
    {
        return new MultidimensionalPrimitiveArraySetter(array);
    }
    
    private static class MultidimensionalPrimitiveArraySetter implements MatlabTypeSerializedSetter
    {
        private final Object _linearArray;
        private final int[] _lengths;
        
        public MultidimensionalPrimitiveArraySetter(Object multidimensionalArray)
        {
            LinearizedArray arrayInfo = linearize(multidimensionalArray);
            _linearArray = arrayInfo.array;
            _lengths = arrayInfo.lengths;
        }
        
        @Override
        public void setInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException
        {
            proxy.setVariable(variableName, this);
            String command = variableName + " = reshape(" + variableName + ".getLinearArray()";
            for(int length : _lengths)
            {
                command += ", " + length;
            }
            command += ");";
            proxy.eval(command);
        }
        
        /**
         * This round about way allows for setting char[] and long[].
         * 
         * @return 
         */
        public Object getLinearArray()
        {
            return _linearArray;
        }
    }
    
    
    private static class LinearizedArray
    {
        /**
         * The linearized array, one of:
         * byte[], short[], int[], long[], float[], double[], char[], boolean[]
         */
        final Object array;
        
        /**
         * The bounding lengths of the array used to multidimensionalize the linearized array.
         */
        final int[] lengths;
        
        private LinearizedArray(Object array, int[] lengths)
        {
            this.array = array;
            this.lengths = lengths;
        }
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
    private static LinearizedArray linearize(Object array)
    {
        //Create linear array with equal size of the array
        int[] lengths = computeBoundingLengths(array);
        int size = getTotalSize(lengths);
        Class<?> baseClass = getBaseArrayClass(array.getClass());
        Object linearArray = Array.newInstance(baseClass, size);
        
        //Fill linearArray with values from array
        ArrayFillOperation fillOperation = FILL_OPERATIONS.get(baseClass);
        linearize_internal(linearArray, array, fillOperation, lengths, new int[0]);
       
        return new LinearizedArray(linearArray, lengths);
    }
    
    
    private static Class<?> getBaseArrayClass(Class<?> type)
    {   
        while(type.isArray())
        {
            type = type.getComponentType();
        }
        
        return type;
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
     * Determines the maximum length for each dimension of the array.
     * 
     * @param array
     * @return 
     */
    private static int[] computeBoundingLengths(Object array)
    {
        int[] maxLengths = new int[getNumberOfDimensions(array.getClass())];

        //The length of this array
        int arrayLength = Array.getLength(array);
        maxLengths[0] = arrayLength;

        //If the array holds arrays as its entries
        if(array.getClass().getComponentType().isArray())
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
     * Performs the linearization using recursion.
     * 
     * @param linearArray array to be filled
     * @param srcArray source array
     * @param fillOperation  operation to read values from srcArray and write into linearArray
     * @param lengths the lengths of the array for the initial array supplied before any recursion
     * @param currIndices must initially be an empty integer array
     */
    private static void linearize_internal(Object linearArray, Object srcArray, ArrayFillOperation fillOperation,
            int[] lengths, int[] currIndices)
    {
        //Base case
        if(!srcArray.getClass().getComponentType().isArray())
        {
            //Fill linear array with contents of the source array
            int[] primArrayIndices = new int[currIndices.length + 1];
            System.arraycopy(currIndices, 0, primArrayIndices, 0, currIndices.length);
            fillOperation.fill(linearArray, srcArray, primArrayIndices, lengths);
        }
        else
        {
            int arrayLength = Array.getLength(srcArray);
            for(int i = 0; i < arrayLength; i++)
            {
                int[] nextIndices = new int[currIndices.length + 1];
                System.arraycopy(currIndices, 0, nextIndices, 0, currIndices.length);
                nextIndices[nextIndices.length - 1] = i;
                
                linearize_internal(linearArray, Array.get(srcArray, i), fillOperation, lengths, nextIndices);
            }
        }
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
    
    private static final Map<Class<?>, ArrayFillOperation<?>> FILL_OPERATIONS = new ConcurrentHashMap<Class<?>, ArrayFillOperation<?>>();
    static
    {
        FILL_OPERATIONS.put(byte.class, new ByteArrayFillOperation());
        FILL_OPERATIONS.put(short.class, new ShortArrayFillOperation());
        FILL_OPERATIONS.put(int.class, new IntArrayFillOperation());
        FILL_OPERATIONS.put(long.class, new LongArrayFillOperation());
        FILL_OPERATIONS.put(float.class, new FloatArrayFillOperation());
        FILL_OPERATIONS.put(double.class, new DoubleArrayFillOperation());
        FILL_OPERATIONS.put(boolean.class, new BooleanArrayFillOperation());
        FILL_OPERATIONS.put(char.class, new CharArrayFillOperation());
    }
    
    private static interface ArrayFillOperation<T>
    {
        public void fill(T linearArray, T arrayPiece, int[] primArrayIndices, int[] lengths);
    }
    
    private static class ByteArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<byte[]>
    {
        @Override
        public void fill(byte[] linearArray, byte[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class ShortArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<short[]>
    {
        @Override
        public void fill(short[] linearArray, short[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class IntArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<int[]>
    {
        @Override
        public void fill(int[] linearArray, int[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class LongArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<long[]>
    {
        @Override
        public void fill(long[] linearArray, long[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class FloatArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<float[]>
    {
        @Override
        public void fill(float[] linearArray, float[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class DoubleArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<double[]>
    {
        @Override
        public void fill(double[] linearArray, double[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class BooleanArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<boolean[]>
    {
        @Override
        public void fill(boolean[] linearArray, boolean[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
    
    private static class CharArrayFillOperation implements ArrayLinearizer.ArrayFillOperation<char[]>
    {
        @Override
        public void fill(char[] linearArray, char[] arrayPiece, int[] primArrayIndices, int[] lengths)
        {
            for(int i = 0; i < arrayPiece.length; i++)
            {
                primArrayIndices[primArrayIndices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, primArrayIndices);
                linearArray[linearIndex] = arrayPiece[i];
            }
        }       
    }
}