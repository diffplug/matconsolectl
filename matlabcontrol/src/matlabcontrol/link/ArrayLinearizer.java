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

import java.util.HashMap;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Map;

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
            _lengths = computeBoundingDimensions(multidimensionalArray);
            _linearArray = linearize(multidimensionalArray, _lengths);
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
    
    /**
     * Creates a linearized version of the {@code array}.
     * 
     * @param array a multidimensional primitive array
     * @param lengths the bounding lengths of the array
     * @return linear array
     */
    static Object linearize(Object array, int[] lengths)
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
        int size = getNumberOfElements(lengths);
        Object linearArray = Array.newInstance(baseClass, size);
        
        //Fill linearArray with values from array
        ArrayFillOperation fillOperation = FILL_OPERATIONS.get(baseClass);
        linearize_internal(linearArray, array, fillOperation, lengths, 0, new int[lengths.length]);
       
        return linearArray;
    }
        
    /**
     * Performs the linearization using recursion.
     * 
     * @param linearArray array to be filled
     * @param srcArray source array
     * @param fillOperation operation to read values from srcArray and write into linearArray
     * @param lengths the lengths of the array for the initial array supplied before any recursion
     * @param depth the level of recursion, initially {@code 0}
     * @param indices must be the length of {@code lengths}
     */
    private static void linearize_internal(Object linearArray, Object srcArray, ArrayFillOperation fillOperation,
            int[] lengths, int depth, int[] indices)
    {
        //Base case - array holding non-array elements
        if(!srcArray.getClass().getComponentType().isArray())
        {
            fillOperation.fill(linearArray, srcArray, indices, lengths);
        }
        else
        {
            int arrayLength = Array.getLength(srcArray);
            for(int i = 0; i < arrayLength; i++)
            {
                indices[depth] = i;
                linearize_internal(linearArray, Array.get(srcArray, i), fillOperation, lengths, depth + 1, indices);
            }
        }
    }
    
    private static final Map<Class<?>, ArrayFillOperation<?>> FILL_OPERATIONS;
    static
    {
        Map<Class<?>, ArrayFillOperation<?>> map = new HashMap<Class<?>, ArrayFillOperation<?>>();
        
        map.put(byte.class, new ByteArrayFillOperation());
        map.put(short.class, new ShortArrayFillOperation());
        map.put(int.class, new IntArrayFillOperation());
        map.put(long.class, new LongArrayFillOperation());
        map.put(float.class, new FloatArrayFillOperation());
        map.put(double.class, new DoubleArrayFillOperation());
        map.put(boolean.class, new BooleanArrayFillOperation());
        map.put(char.class, new CharArrayFillOperation());
        
        FILL_OPERATIONS = Collections.unmodifiableMap(map);
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