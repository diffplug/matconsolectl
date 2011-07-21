package matlabcontrol.link;

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
import matlabcontrol.MatlabOperations;
import matlabcontrol.link.MatlabType.MatlabTypeSetter;
import static matlabcontrol.link.ArrayTransformUtils.*;

/**
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayLinearizer
{   
    static MatlabTypeSetter getSetter(Object array)
    {
        return new MultidimensionalPrimitiveArraySetter(array);
    }
    
    private static class MultidimensionalPrimitiveArraySetter implements MatlabTypeSetter
    {
        private final Object _linearArray;
        private final int[] _lengths;
        
        public MultidimensionalPrimitiveArraySetter(Object multidimensionalArray)
        {
            LinearizedArray arrayInfo = linearize(multidimensionalArray, null);
            _linearArray = arrayInfo.array;
            _lengths = arrayInfo.lengths;
        }
        
        @Override
        public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            ops.setVariable(variableName, this);
            
            String command;
            if(_lengths.length == 1)
            {
                command = variableName + " = reshape(" + variableName + ".getLinearArray(), 1, " + _lengths[0] + ");";
            }
            else
            {
                command = variableName + " = reshape(" + variableName + ".getLinearArray()";
                for(int length : _lengths)
                {
                    command += ", " + length;
                }
                command += ");";
            }

            ops.eval(command);
        }
        
        /**
         * This round about way allows for setting char[] and long[] without exception.
         * 
         * @return 
         */
        public Object getLinearArray()
        {
            return _linearArray;
        }
    }
    
    static class LinearizedArray
    {
        /**
         * The linearized array, one of:
         * byte[], short[], int[], long[], float[], double[], char[], boolean[]
         */
        final Object array;
        
        /**
         * The bounding lengths of the array used to reshape the linearized array in MATLAB.
         */
        final int[] lengths;
        
        private LinearizedArray(Object array, int[] lengths)
        {
            this.array = array;
            this.lengths = lengths;
        }
    }
    
    /**
     * Creates a linearized version of the {@code array}. The bounding length of each dimension is used if the array is
     * jagged.
     * 
     * @param array a multidimensional primitive array
     * @param lengths the lengths of the array, if null then the lengths of the array will be computed
     * @return linear array and lengths used
     */
    static LinearizedArray linearize(Object array, int[] lengths)
    {
        if(array == null | !array.getClass().isArray())
        {
            throw new RuntimeException("provided object is not an array");
        }
        
        //Base component type of the array
        Class<?> baseClass = getBaseComponentType(array.getClass());
        if(!baseClass.isPrimitive())
        {
            throw new RuntimeException("array type is not a primitive, type: " + baseClass.getCanonicalName());
        }
        
        //Create linear array with size equal to that of the bounding lengths of the array
        lengths = (lengths == null ? computeBoundingLengths(array) : lengths);
        int size = getTotalSize(lengths);
        Object linearArray = Array.newInstance(baseClass, size);
        
        //Fill linearArray with values from array
        ArrayFillOperation fillOperation = FILL_OPERATIONS.get(baseClass);
        linearize_internal(linearArray, array, fillOperation, lengths, new int[0]);
       
        return new LinearizedArray(linearArray, lengths);
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
        public void fill(T dst, T src, int[] indices, int[] lengths);
    }
    
    private static class ByteArrayFillOperation implements ArrayFillOperation<byte[]>
    {
        @Override
        public void fill(byte[] dst, byte[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class ShortArrayFillOperation implements ArrayFillOperation<short[]>
    {
        @Override
        public void fill(short[] dst, short[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class IntArrayFillOperation implements ArrayFillOperation<int[]>
    {
        @Override
        public void fill(int[] dst, int[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class LongArrayFillOperation implements ArrayFillOperation<long[]>
    {
        @Override
        public void fill(long[] dst, long[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class FloatArrayFillOperation implements ArrayFillOperation<float[]>
    {
        @Override
        public void fill(float[] dst, float[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class DoubleArrayFillOperation implements ArrayFillOperation<double[]>
    {
        @Override
        public void fill(double[] dst, double[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class BooleanArrayFillOperation implements ArrayFillOperation<boolean[]>
    {
        @Override
        public void fill(boolean[] dst, boolean[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
    
    private static class CharArrayFillOperation implements ArrayFillOperation<char[]>
    {
        @Override
        public void fill(char[] dst, char[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < src.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[linearIndex] = src[i];
            }
        }       
    }
}