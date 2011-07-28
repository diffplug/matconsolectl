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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions used to multidimensionalize and linearize arrays.
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayTransformUtils
{
    /**
     * Deeply copies a primitive array. If the array is not primitive then the {@code Object}s in the array will not
     * be copies, although the arrays will be copied.
     * 
     * @param <T>
     * @param array
     * @return copy of array
     */
    static <T> T deepCopy(T array)
    {
        T copy;
        
        if(array == null)
        {
            copy = null;
        }
        else if(array.getClass().isArray())
        {
            //Array of arrays
            if(array.getClass().getComponentType().isArray())
            {
                int arrayLength = Array.getLength(array);
                copy = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength);
                for(int i = 0; i < arrayLength; i++)
                {
                    Array.set(copy, i, deepCopy(Array.get(array, i)));
                }
            }
            //Array of values
            else
            {
                int arrayLength = Array.getLength(array);
                copy = (T) Array.newInstance(array.getClass().getComponentType(), arrayLength);
                System.arraycopy(array, 0, copy, 0, arrayLength);
            }
        }
        else
        {
            throw new IllegalArgumentException("Input not an array: " + array.getClass().getCanonicalName());
        }
        
        return copy;
    }
    
    /**
     * Computes the total number of elements in an array with dimensions specified by {@code dimensions}.
     * 
     * @param dimensions
     * @return 
     */
    static int getNumberOfElements(int[] dimensions)
    {
        int size = 0;
        
        for(int length : dimensions)
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
     * Multidimensional indices to linear index. Similar to MATLAB's (@code sub2ind} function. The lengths of
     * {@code dimensions} and {@code indices} should be the same; this is <b>not</b> checked.
     * 
     * @param dimensions the lengths of the array in each dimension
     * @param indices
     * @return linear index
     */
    static int multidimensionalIndicesToLinearIndex(int[] dimensions, int[] indices)
    {
        int linearIndex = 0;
        
        int accumSize = 1;
        for(int i = 0; i < dimensions.length; i++)
        {   
            linearIndex += accumSize * indices[i];
            accumSize *= dimensions[i];
        }
        
        return linearIndex;
    }
    
    //Optimized version for 2 indices
    static int multidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column)
    {
        return column * dimensions[0] + row;
    }
    
    //Optimized version for 3 indices
    static int multidimensionalIndicesToLinearIndex(int[] dimensions, int row, int column, int page)
    {
        return page * (dimensions[0] * dimensions[1]) + column * dimensions[0] + row;
    }
    
    /**
     * Linear index to multidimensional indices. Similar to MATLAB's (@code ind2sub} function.
     * 
     * @param dimensions the lengths of the array in each dimension
     * @param linearIndex
     * @return 
     */
    static int[] linearIndexToMultidimensionalIndices(int[] dimensions, int linearIndex)
    {
        int[] indices = new int[dimensions.length];
        
        if(dimensions.length == 1)
        {
            indices[0] = linearIndex;
        }
        else
        {
            int pageSize = dimensions[0] * dimensions[1];
            int pageNumber = linearIndex / pageSize;

            //Row and column
            int indexInPage = linearIndex % pageSize;
            indices[0] = indexInPage % dimensions[0];
            indices[1] = indexInPage / dimensions[0];

            //3rd dimension and above
            int accumSize = 1;
            for(int dim = 2; dim < dimensions.length; dim++)
            {
                indices[dim] = (pageNumber / accumSize) % dimensions[dim];
                accumSize *= dimensions[dim];
            }
        }
        
        return indices;
    }
    
    /**
     * Gets the base component type of {@code type} assuming {@code type} is a type of array. If {@code type} is not
     * an array then the same class passed in will be returned. For example, if {@code type} is {@code boolean[][]} then
     * {@code boolean} would be returned.
     * 
     * @param type
     * @return 
     */
    static Class<?> getBaseComponentType(Class<?> type)
    {   
        while(type.isArray())
        {
            type = type.getComponentType();
        }
        
        return type;
    }
    
    /**
     * Gets the number of dimensions represented by {@code type}. If not an array then {@code 0} will be returned.
     * 
     * @param type
     * @return 
     */
    static int getNumberOfDimensions(Class<?> type)
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
     * Determines the maximum length for each dimension of the array.
     * 
     * @param array
     * @return 
     */
    static int[] computeBoundingDimensions(Object array)
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
                int[] childLengths = computeBoundingDimensions(Array.get(array, i));
                for(int j = 0; j < childLengths.length; j++)
                {
                    maxLengths[j + 1] = Math.max(maxLengths[j + 1], childLengths[j]);
                }
            }
        }
        
        return maxLengths;
    }
    
    /**
     * Gets the class representing an array of type {@code componentType} with the number of dimensions specified by
     * {@code rank}. JVMs typically impose a limit of 255 dimensions.
     * 
     * @param componentType
     * @param rank
     * @return 
     */
    static Class<?> getArrayClass(Class<?> componentType, int rank)
    {
        String binaryName;
        if(componentType.isPrimitive())
        {
            binaryName = getPrimitiveArrayBinaryName(componentType, rank);
        }
        else
        {
            binaryName = getObjectArrayBinaryName(componentType, rank);
        }
        
        try
        {
            return Class.forName(binaryName, false, componentType.getClassLoader());
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not create array class\n" +
                    "Component Type (Canonical Name): " + componentType.getCanonicalName() + "\n" +
                    "Component Type (Name): " + componentType.getName() + "\n" +
                    "Rank: " + rank + "\n" +
                    "Array Class Binary Name: "+ binaryName, e);
        }
    }
    
    private static String getPrimitiveArrayBinaryName(Class<?> componentType, int rank)
    {
        //Build binary name
        char[] nameChars = new char[rank + 1];
        for(int i = 0; i < rank; i++)
        {
            nameChars[i] = '[';
        }
        nameChars[nameChars.length - 1] = PRIMITIVE_TO_BINARY_NAME.get(componentType);
        
        return new String(nameChars);
    }
    
    private static final Map<Class<?>, Character> PRIMITIVE_TO_BINARY_NAME;
    static
    {
        Map<Class<?>, Character> map = new HashMap<Class<?>, Character>();
        
        map.put(byte.class, 'B');
        map.put(short.class, 'S');
        map.put(int.class, 'I');
        map.put(long.class, 'J');
        map.put(float.class, 'F');
        map.put(double.class, 'D');
        map.put(boolean.class, 'Z');
        map.put(char.class, 'C');
        
        PRIMITIVE_TO_BINARY_NAME = Collections.unmodifiableMap(map);
    }
    
    private static String getObjectArrayBinaryName(Class<?> componentType, int rank)
    {
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < rank; i++)
        {
            builder.append("[");
        }
        builder.append("L");
        builder.append(componentType.getName());
        builder.append(";");
        
        return builder.toString();
    }
}