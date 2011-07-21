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

/**
 * Utility functions used to multidimensionalize and linearize arrays.
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ArrayTransformUtils
{
        
    /**
     * Computes the total size of an array with lengths specified by {@code lengths}.
     * 
     * @param lengths
     * @return 
     */
    static int getTotalSize(int[] lengths)
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
     * Multidimensional indices to linear index. Similar to MATLAB's (@code sub2ind} function.
     * 
     * @param lengths the lengths of the array in each dimension
     * @param indices
     * @return
     * @throws IllegalArgumentException thrown if the length of {@code lengths} and {@code indices} are not the same
     */
    static int multidimensionalIndicesToLinearIndex(int[] lengths, int[] indices)
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
     * Linear index to multidimensional indices. Similar to MATLAB's (@code ind2sub} function.
     * 
     * @param lengths the lengths of the array in each dimension
     * @param linearIndex
     * @return 
     */
    static int[] linearIndexToMultidimensionalIndices(int[] lengths, int linearIndex)
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
    static int[] computeBoundingLengths(Object array)
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
    
    /**
     * Gets the class representing an array of type {@code componentType} with the number of dimensions specified by
     * {@code dimensions}. JVMs typically impose a limit of 255 dimensions.
     * 
     * @param componentType
     * @param dimensions
     * @return 
     */
    static Class<?> getMultidimensionalArrayClass(Class<?> componentType, int dimensions)
    {
        String binaryName;
        if(componentType.isPrimitive())
        {
            binaryName = getPrimitiveMultidimensionalBinaryName(componentType, dimensions);
        }
        else
        {
            binaryName = getObjectMultidimensionalBinaryName(componentType, dimensions);
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
                    "Dimensions: " + dimensions + "\n" +
                    "Array Class Binary Name: "+ binaryName, e);
        }
    }
    
    private static String getPrimitiveMultidimensionalBinaryName(Class<?> componentType, int dimensions)
    {
        //Build binary name
        char[] nameChars = new char[dimensions + 1];
        for(int i = 0; i < dimensions; i++)
        {
            nameChars[i] = '[';
        }
        nameChars[nameChars.length - 1] = PRIMITIVE_TO_BINARY_NAME.get(componentType);
        String binaryName = new String(nameChars);
        
        return binaryName;
    }
    
    private static String getObjectMultidimensionalBinaryName(Class<?> componentType, int dimensions)
    {
        String binaryName = "";
        for(int i = 0; i < dimensions; i++)
        {
            binaryName += "[";
        }
        binaryName += "L";
        binaryName += componentType.getName();
        binaryName += ";";
        
        return binaryName;
    }
    
    private static final Map<Class<?>, Character> PRIMITIVE_TO_BINARY_NAME = new ConcurrentHashMap<Class<?>, Character>();
    static
    {
        PRIMITIVE_TO_BINARY_NAME.put(byte.class, 'B');
        PRIMITIVE_TO_BINARY_NAME.put(short.class, 'S');
        PRIMITIVE_TO_BINARY_NAME.put(int.class, 'I');
        PRIMITIVE_TO_BINARY_NAME.put(long.class, 'J');
        PRIMITIVE_TO_BINARY_NAME.put(float.class, 'F');
        PRIMITIVE_TO_BINARY_NAME.put(double.class, 'D');
        PRIMITIVE_TO_BINARY_NAME.put(boolean.class, 'Z');
        PRIMITIVE_TO_BINARY_NAME.put(char.class, 'C');
    }
}