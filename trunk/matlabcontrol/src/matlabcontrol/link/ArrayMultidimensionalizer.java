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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import matlabcontrol.MatlabOperations;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.link.MatlabType.MatlabTypeGetter;
import static matlabcontrol.link.ArrayTransformUtils.*;

/**
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayMultidimensionalizer
{   
    static class PrimitiveArrayGetter implements MatlabTypeGetter
    {
        //Note: uint8, uint16, uint32, and uint64 are intentionally not supported because MATLAB will convert them
        //to byte, short, int, and long but that won't work properly for large values due to having one less bit
        private static final Set<String> SUPPORTED_MATLAB_CLASSES;
        static
        {
            Set<String> set = new HashSet<String>();
            
            set.add("int8");
            set.add("int16");
            set.add("int32");
            set.add("int64");
            set.add("single");
            set.add("double");
            set.add("logical");
            set.add("char");
            
            SUPPORTED_MATLAB_CLASSES = Collections.unmodifiableSet(set);
        }
        
        private int[] _lengths;
        private Object _array;
        private IncompatibleReturnException _exception;
        
        private boolean _retreived = false;
        
        private final boolean _getRealPart;
        private final boolean _keepLinear;
        
        public PrimitiveArrayGetter(boolean realPart, boolean keepLinear)
        {
            _getRealPart = realPart;
            _keepLinear = keepLinear;
        }
        
        public boolean isRealPart()
        {
            return _getRealPart;
        }
        
        public int[] getLengths()
        {
            return _lengths;
        }
        
        /**
         * May be null if this represents the imaginary part and the array has no imaginary values or the array was
         * empty.
         * 
         * @return 
         */
        @Override
        public Object retrieve()
        {
            //Validate
            if(_retreived)
            {
                if(_exception != null)
                {
                    _exception.fillInStackTrace();
                    throw _exception;
                }
            }
            else
            {
                throw new IllegalStateException("array has not yet been retrieved");
            }
            
            if(_array != null && !_keepLinear)
            {
                _array = multidimensionalize(_array, _lengths);
            }
            
            return _array;
        }

        @Override
        public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            //Type
            String type = (String) ops.returningEval("class(" + variableName + ");", 1)[0];
            if(!SUPPORTED_MATLAB_CLASSES.contains(type))
            {
                _exception = new IncompatibleReturnException("Type not supported: " + type + "\n" +
                        "Supported Types: " + SUPPORTED_MATLAB_CLASSES);
            }
            else
            {
                //If an empty array, equivalent to Java null
                if(((boolean[]) ops.returningEval("isempty(" + variableName + ");", 1)[0])[0])
                {
                    _array = null;
                }
                //If a scalar (singular) value
                else if(((boolean[]) ops.returningEval("isscalar(" + variableName + ");", 1)[0])[0])
                {
                    _exception = new IncompatibleReturnException("Scalar value of type " + type);
                }
                else
                {
                    //Retrieve lengths of array
                    double[] size = (double[]) ops.returningEval("size(" + variableName + ");", 1)[0];
                    _lengths = new int[size.length];
                    for(int i = 0; i < size.length; i++)
                    {
                        _lengths[i] = (int) size[i];
                    }
                    
                    //Retrieve array
                    String name = (String) ops.returningEval("genvarname('" + variableName + "_getter', who);", 1)[0];
                    ops.setVariable(name, this);
                    try
                    {
                        if(_getRealPart)
                        {
                            ops.eval(name + ".setArray(reshape(" + variableName + ", 1, []));");
                        }
                        else
                        {
                            //If the array has an imaginary part, retrieve it
                            boolean isReal = ((boolean[]) ops.returningEval("isreal(" + variableName + ");", 1)[0])[0];
                            if(!isReal)
                            {
                                ops.eval(name + ".setArray(imag(reshape(" + variableName + ", 1, [])));");
                            }
                        }
                    }
                    finally
                    {
                        ops.eval("clear " + name);
                    }
                }
            }
            
            _retreived = true;
        }
        
        //The following methods are to be called only from MATLAB to pass in the array
        
        public void setArray(byte[] array)
        {
            _array = array;
        }
        
        public void setArray(short[] array)
        {
            _array = array;
        }
        
        public void setArray(int[] array)
        {
            _array = array;
        }
        
        public void setArray(long[] array)
        {
            _array = array;
        }
        
        public void setArray(float[] array)
        {
            _array = array;
        }
        
        public void setArray(double[] array)
        {
            _array = array;
        }
        
        public void setArray(char[] array)
        {
            _array = array;
        }
        
        public void setArray(boolean[] array)
        {
            _array = array;
        }
    }
    
    /**
     * Creates a rectangular multidimensional array from {@code linearArray} with the length of each dimension specified
     * by {@code lengths}. The length of the array must equal the total size specified by {@code lengths}.
     * 
     * @param <T>
     * @param linearArray a primitive linear array
     * @param lengths length of each dimension
     * @return multidimensional array
     */
    static Object multidimensionalize(Object linearArray, int[] lengths)
    {
        //Validate primitive single dimension array
        if(linearArray == null || !linearArray.getClass().isArray() &&
                !linearArray.getClass().getComponentType().isPrimitive())
        {
            throw new RuntimeException("linear array must be a single dimension primitive array");
        }
        
        //Validate lengths
        if(getTotalSize(lengths) != Array.getLength(linearArray))
        {
            throw new RuntimeException("linear array's length does not match the total size of the provided " +
                    "multidimensional lengths\n"+
                    "Linear Array Length: " + Array.getLength(linearArray) + "\n" +
                    "Multidimensional Lengths: " + Arrays.toString(lengths) + "\n" +
                    "Multidimensional Total Size: " + getTotalSize(lengths));
        }
        
        Class componentType = linearArray.getClass().getComponentType();
        Class outputArrayType = getMultidimensionalArrayClass(componentType, lengths.length);
        ArrayFillOperation fillOperation = FILL_OPERATIONS.get(componentType);
        
        return multidimensionalize_internal(linearArray, outputArrayType, lengths, 0, new int[0], fillOperation);
    }
    
    /**
     * The real logic of the {@link #multidimensionalize(double[], java.lang.Class, int[])} method. This method
     * recurs on itself, hence the need for the extra parameters. This method does not store state in any external
     * variables.
     * 
     * @param linearArray
     * @param outputArrayType
     * @param lengths
     * @param indexIntoLengths should be {@code 0} initially
     * @param currIndices should be an empty integer array initially
     * @param fillOperation operation used to fill in single dimension pieces of the array
     * @return
     */
    private static Object multidimensionalize_internal(Object linearArray, Class<?> outputArrayType, int[] lengths,
            int indexIntoLengths, int[] currIndices, ArrayFillOperation fillOperation)
    {
        Class<?> componentType = outputArrayType.getComponentType();
        int arrayLength = lengths[indexIntoLengths];
        Object array = Array.newInstance(componentType, arrayLength);
        
        //If what was created holds an array, then fill it
        if(componentType.isArray())
        {
            //If the array that was created holds the primitive array: double[], int[], boolean[] etc.
            if(componentType.getComponentType().isPrimitive())
            {
                //The index in the multidimensional array being created
                int[] primitiveArrayIndices = new int[currIndices.length + 2];
                System.arraycopy(currIndices, 0, primitiveArrayIndices, 0, currIndices.length);
                
                //Iterate over the created array, placing a double[] array in each index
                for(int i = 0; i < arrayLength; i++)
                {
                    primitiveArrayIndices[primitiveArrayIndices.length - 2] = i;
                
                    //Create the primitive array
                    Object primitiveArray = Array.newInstance(componentType.getComponentType(), lengths[lengths.length - 1]); 
                    
                    //Fill the primitive array with values from the linear array
                    fillOperation.fill(primitiveArray, linearArray, primitiveArrayIndices, lengths);
                    
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
                    
                    Object innerArray = multidimensionalize_internal(linearArray, componentType, lengths,
                            indexIntoLengths + 1, nextIndices, fillOperation);
                    Array.set(array, i, innerArray);
                }
            }
        }
        //If the componentType is not an array, then the original request was to create a one dimensional array
        else
        {
            System.arraycopy(linearArray, 0, array, 0, arrayLength);
        }
        
        return array;
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
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class ShortArrayFillOperation implements ArrayFillOperation<short[]>
    {
        @Override
        public void fill(short[] dst, short[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class IntArrayFillOperation implements ArrayFillOperation<int[]>
    {
        @Override
        public void fill(int[] dst, int[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class LongArrayFillOperation implements ArrayFillOperation<long[]>
    {
        @Override
        public void fill(long[] dst, long[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class FloatArrayFillOperation implements ArrayFillOperation<float[]>
    {
        @Override
        public void fill(float[] dst, float[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class DoubleArrayFillOperation implements ArrayFillOperation<double[]>
    {
        @Override
        public void fill(double[] dst, double[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class BooleanArrayFillOperation implements ArrayFillOperation<boolean[]>
    {
        @Override
        public void fill(boolean[] dst, boolean[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
    
    private static class CharArrayFillOperation implements ArrayFillOperation<char[]>
    {
        @Override
        public void fill(char[] dst, char[] src, int[] indices, int[] lengths)
        {
            for(int i = 0; i < dst.length; i++)
            {
                indices[indices.length - 1] = i;
                int linearIndex = multidimensionalIndicesToLinearIndex(lengths, indices);
                dst[i] = src[linearIndex];
            }
        }       
    }
}