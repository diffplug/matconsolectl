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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class that operates on arrays which supports passing in array objects with types that are not known at
 * compile time. Some of these methods are thin wrappers around methods in {@link java.util.Arrays}.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class DynamicArrays
{
    private DynamicArrays() { }
    
    /**
     * Hashes an array. Provides a deep hash code in the case of an object array that contains other arrays.
     * 
     * @param array must be an array of any type or {@code null}
     * @return hashCode
     */
    static int hashCode(Object array)
    {
        int hashCode;
        if(array == null)
        {
            hashCode = 0;
        }
        else if(array instanceof byte[])
        {
            hashCode = Arrays.hashCode((byte[]) array);
        }
        else if(array instanceof short[])
        {
            hashCode = Arrays.hashCode((short[]) array);
        }
        else if(array instanceof int[])
        {
            hashCode = Arrays.hashCode((int[]) array);
        }
        else if(array instanceof long[])
        {
            hashCode = Arrays.hashCode((long[]) array);
        }
        else if(array instanceof float[])
        {
            hashCode = Arrays.hashCode((float[]) array);
        }
        else if(array instanceof double[])
        {
            hashCode = Arrays.hashCode((double[]) array);
        }
        else if(array instanceof boolean[])
        {
            hashCode = Arrays.hashCode((boolean[]) array);
        }
        else if(array instanceof char[])
        {
            hashCode = Arrays.hashCode((char[]) array);
        }
        //This is true if array is any non-primitive array
        else if(array instanceof Object[])
        {
            hashCode = Arrays.hashCode((Object[]) array);
        }
        else
        {
            throw new IllegalArgumentException("value provided is not array, class: " +
                    array.getClass().getCanonicalName());
        }
        
        return hashCode;
    }
    
    static boolean equals(Object array1, Object array2)
    {
        boolean equal = false;
        //If the same array or both null
        if(array1 == array2)
        {
            equal = true;
        }
        //If both non-null and of the same class
        else if(array1 != null && array2 != null && array1.getClass().equals(array2.getClass()))
        {
            if(array1 instanceof byte[] && array2 instanceof byte[])
            {
                equal = Arrays.equals((byte[]) array1, (byte[]) array2);
            }
            else if(array1 instanceof short[] && array2 instanceof short[])
            {
                equal = Arrays.equals((short[]) array1, (short[]) array2);
            }
            else if(array1 instanceof int[] && array2 instanceof int[])
            {
                equal = Arrays.equals((int[]) array1, (int[]) array2);
            }
            else if(array1 instanceof long[] && array2 instanceof long[])
            {
                equal = Arrays.equals((byte[]) array1, (byte[]) array2);
            }
            else if(array1 instanceof float[] && array2 instanceof float[])
            {
                equal = Arrays.equals((float[]) array1, (float[]) array2);
            }
            else if(array1 instanceof double[] && array2 instanceof double[])
            {
                equal = Arrays.equals((double[]) array1, (double[]) array2);
            }
            else if(array1 instanceof boolean[] && array2 instanceof boolean[])
            {
                equal = Arrays.equals((boolean[]) array1, (boolean[]) array2);
            }
            else if(array1 instanceof char[] && array2 instanceof char[])
            {
                equal = Arrays.equals((char[]) array1, (char[]) array2);
                
            }
            else if(array1 instanceof Object[] && array2 instanceof Object[])
            {
                equal = Arrays.equals((Object[]) array1, (Object[]) array2); 
            }
            else
            {
                throw new IllegalArgumentException("One or more of the values provided are not an array\n" +
                        "array1 class: " + array1.getClass() + "\n" +
                        "array2 class: " + array2.getClass());
            }
        }
        
        return equal;   
    }
    
    static boolean containsNonDefaultValue(Object array)
    {
        boolean contains;
        if(array == null)
        {
            throw new NullPointerException("array may not be null");
        }
        else if(!array.getClass().isArray())
        {
            throw new IllegalArgumentException("value provided is not an array, class: " + array.getClass());
        }
        else
        {
            ArrayContainmentOperation operation;
            if(array.getClass().getComponentType().isPrimitive())
            {
                operation = CONTAINMENT_OPERATIONS.get(array.getClass().getComponentType());
            }
            else
            {
                operation = CONTAINMENT_OPERATIONS.get(Object[].class);
            }
            
            contains = operation.containsNonDefaultValue(array);
        }
        
        return contains;
    }
    
    private static final Map<Class<?>, ArrayContainmentOperation<?>> CONTAINMENT_OPERATIONS;
    static
    {
        Map<Class<?>, ArrayContainmentOperation<?>> map = new HashMap<Class<?>, ArrayContainmentOperation<?>>();
        
        map.put(byte[].class, new ByteArrayContainmentOperation());
        map.put(short[].class, new ShortArrayContainmentOperation());
        map.put(int[].class, new IntArrayContainmentOperation());
        map.put(long[].class, new LongArrayContainmentOperation());
        map.put(float[].class, new FloatArrayContainmentOperation());
        map.put(double[].class, new DoubleArrayContainmentOperation());
        map.put(boolean[].class, new BooleanArrayContainmentOperation());
        map.put(char[].class, new CharArrayContainmentOperation());
        map.put(Object[].class, new ObjectArrayContainmentOperation());
        
        CONTAINMENT_OPERATIONS = Collections.unmodifiableMap(map);
    }
    
    private static interface ArrayContainmentOperation<T>
    {
        public boolean containsNonDefaultValue(T array);
    }
    
    private static class ByteArrayContainmentOperation implements ArrayContainmentOperation<byte[]>
    {
        private static final byte DEFAULT_VAL = (byte) 0;
        
        @Override
        public boolean containsNonDefaultValue(byte[] array)
        {
            boolean contains = false;
            
            for(byte val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class ShortArrayContainmentOperation implements ArrayContainmentOperation<short[]>
    {
        private static final short DEFAULT_VAL = (short) 0;
        
        @Override
        public boolean containsNonDefaultValue(short[] array)
        {
            boolean contains = false;
            
            for(short val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class IntArrayContainmentOperation implements ArrayContainmentOperation<int[]>
    {
        private static final int DEFAULT_VAL = 0;
        
        @Override
        public boolean containsNonDefaultValue(int[] array)
        {
            boolean contains = false;
            
            for(int val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class LongArrayContainmentOperation implements ArrayContainmentOperation<long[]>
    {
        private static final long DEFAULT_VAL = 0L;
        
        @Override
        public boolean containsNonDefaultValue(long[] array)
        {
            boolean contains = false;
            
            for(long val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class FloatArrayContainmentOperation implements ArrayContainmentOperation<float[]>
    {
        private static final float DEFAULT_VAL = 0f;
        
        @Override
        public boolean containsNonDefaultValue(float[] array)
        {
            boolean contains = false;
            
            for(float val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class DoubleArrayContainmentOperation implements ArrayContainmentOperation<double[]>
    {
        private static final double DEFAULT_VAL = 0d;
        
        @Override
        public boolean containsNonDefaultValue(double[] array)
        {
            boolean contains = false;
            
            for(double val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class BooleanArrayContainmentOperation implements ArrayContainmentOperation<boolean[]>
    {
        private static final boolean DEFAULT_VAL = false;
        
        @Override
        public boolean containsNonDefaultValue(boolean[] array)
        {
            boolean contains = false;
            
            for(boolean val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class CharArrayContainmentOperation implements ArrayContainmentOperation<char[]>
    {
        private static final char DEFAULT_VAL = '\0';
        
        @Override
        public boolean containsNonDefaultValue(char[] array)
        {
            boolean contains = false;
            
            for(char val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
    
    private static class ObjectArrayContainmentOperation implements ArrayContainmentOperation<Object[]>
    {
        private static final Object DEFAULT_VAL = null;
        
        @Override
        public boolean containsNonDefaultValue(Object[] array)
        {
            boolean contains = false;
            
            for(Object val : array)
            {
                if(val != DEFAULT_VAL)
                {
                    contains = true;
                    break;
                }
            }
            
            return contains;
        }
    }
}