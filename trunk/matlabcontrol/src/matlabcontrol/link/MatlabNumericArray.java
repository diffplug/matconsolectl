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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.link.ArrayMultidimensionalizer.PrimitiveArrayGetter;

/**
 * Acts as a MATLAB numeric array of any dimension. This representation is a copy of the MATLAB data, not a live view.
 * Retrieving large arrays from MATLAB can result in a {@link OutOfMemoryError}; if this occurs you may want to either
 * retrieve only part of the array from MATLAB or increase your Java Virtual Machine's heap size.
 * <br><br>
 * MATLAB numeric arrays of {@code int8}, {@code int16}, {@code int32}, {@code int64}, {@code single}, and
 * {@code double} are supported. They will become {@code byte}, {@code short}, {@code int}, {@code long}, {@code float},
 * and {@code double} Java arrays respectively. MATLAB unsigned integer values ({@code uint8}, {@code uint16},
 * {@code uint32}, and {@code uint64}) are not supported. This class may be parameterized with arrays of type
 * {@code byte}, {@code short}, {@code int}, {@code long}, {@code float}, or {@code double} of one or more dimensions.
 * Typical Java Virtual Machine implementations have a limit of 255 array dimensions.
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
 * MATLAB arrays are always two or more dimensions; single dimension Java arrays will become MATLAB arrays of size 1 by
 * <i>n</i> where <i>n</i> is the length of the Java array.
 * <br><br>
 * When an array is retrieved from MATLAB the resulting Java array is never jagged. When a {@code MatlabNumericArray} is
 * constructed from Java arrays, the arrays provided may be jagged. The bounding "box" of the provided array or arrays
 * is used and 0 is placed in all MATLAB array locations which do not have a corresponding Java array location.
 * <br><br>
 * While this class mimics the dimension and lengths of a MATLAB array, it uses Java's zero-based indexing convention
 * instead of MATLAB's one-based convention. For instance in MATLAB if an array were indexed into as
 * {@code array(3,4,7,2)}, then in Java to retrieve the same entry the indexing would be performed as
 * {@code array[2][3][6][1]}.
 * <br><br>
 * Once constructed, this class is unconditionally thread-safe. If the data provided to a constructor is modified while
 * construction is occurring, problems may occur.
 * 
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabNumericArray<T> extends MatlabType
{
    private final Object _real;
    private final Object _imag;
    private final int[] _lengths;
    private final ComplexSupplier _supplier;
    
    /**
     * Data from MATLAB. Provided as the linear arrays as lengths.
     * 
     * @param realLinear
     * @param imagLinear
     * @param lengths 
     */
    private MatlabNumericArray(Object realLinear, Object imagLinear, int[] lengths)
    {
        _real = realLinear;
        _imag = imagLinear;
        _lengths = lengths;
        
        _supplier = COMPLEX_SUPPLIERS.get(_real.getClass().getComponentType());
    }
    
    /**
     * Constructs a numeric array from Java arrays that can be transferred to MATLAB. The {@code imaginary} array
     * may be {@code null}, if so then this array will be real. References to the arrays passed in are not kept, and
     * modifying the array data after this class has been constructed will have no effect. If the data is modified
     * concurrently with this class's construction, problems may arise.
     * <br><br>
     * Example usage:
     * <pre>
     * {@code
     * double[][] real = new double[][] { { 1, 2 }, { 3, 4 } };
     * double[][] imag = new double[][] { { 0, 5 }, { 5, 0 } };
     * MatlabNumericArray<double[][]> matlabArray = new MatlabNumericArray<double[][]>(real, imag);
     * }
     * </pre>
     * 
     * @param real may not be null
     * @param imaginary may be null
     * @throws NullPointerException if real array is null
     * @throws IllegalArgumentException if the arguments are not an array of {@code byte}, {@code short}, {@code int},
     * {@code long}, {@code float}, or {@code double} or the two arrays are of different types
     */
    public MatlabNumericArray(T real, T imaginary)
    {
        if(real == null)
        {
            throw new NullPointerException("Real array may not be null");
        }
        
        Class<?> realClass = real.getClass();
        Class<?> baseComponentType = ArrayTransformUtils.getBaseComponentType(realClass);
        if(!realClass.isArray() || !COMPLEX_SUPPLIERS.containsKey(baseComponentType))
        {
            throw new IllegalArgumentException("Real array is not an array of the required class\n" +
                    "Sypported array base components: " + COMPLEX_SUPPLIERS.keySet() + "\n" +
                    "Real array class: " + realClass.getCanonicalName());
        }
        
        if(imaginary != null && !imaginary.getClass().equals(realClass))
        {
            throw new IllegalArgumentException("Imaginary array is not of the same class as the real array\n" +
                    "Real array class: " + realClass.getCanonicalName() + "\n" +
                    "Imaginary array class: " + imaginary.getClass().getCanonicalName());
        }
        
        //Determine lengths
        _lengths = new int[ArrayTransformUtils.getNumberOfDimensions(realClass)];
        int[] realLengths = ArrayTransformUtils.computeBoundingLengths(real);
        for(int i = 0; i < realLengths.length; i++)
        {
            _lengths[i] = Math.max(_lengths[i], realLengths[i]);
        }
        if(imaginary != null)
        {
            int[] imaginaryLengths = ArrayTransformUtils.computeBoundingLengths(imaginary);
            for(int i = 0; i < imaginaryLengths.length; i++)
            {
                _lengths[i] = Math.max(_lengths[i], imaginaryLengths[i]);
            }
        }
        
        //Linearize arrays
        _real = ArrayLinearizer.linearize(real, _lengths).array;
        if(imaginary != null)
        {
            _imag = ArrayLinearizer.linearize(imaginary, _lengths).array;
        }
        else
        {   
            _imag = null;
        }
        
        _supplier = COMPLEX_SUPPLIERS.get(_real.getClass().getComponentType());
    }
    /**
     * Returns {@code true} if the array has no imaginary values, {@code false} otherwise. Equivalent to the MATLAB
     * {@code isreal} function.
     * 
     * @return 
     */
    public boolean isReal()
    {
        return (_imag == null);
    }
    
    /**
     * Returns an array that holds the real values from the MATLAB array. The returned array is a copy and may be safely
     * modified.
     * 
     * @return real array
     */
    public T toRealArray()
    {
        return (T) ArrayMultidimensionalizer.multidimensionalize(_real, _lengths);
    }
    
    /**
     * Returns an array that holds the imaginary values from the MATLAB array. If no the array has imaginary values then
     * {@code null} will be returned. The returned array is a copy and may be safely modified.
     * 
     * @return imaginary array
     */
    public T toImaginaryArray()
    {
        T array = null;
        if(!isReal())
        {
            array = (T) ArrayMultidimensionalizer.multidimensionalize(_imag, _lengths);
        }
        
        return array;
    }
    
    /**
     * The number of elements in the array.
     * 
     * @return size
     */
    public int size()
    {
        return Array.getLength(_real);
    }
    
    /**
     * Returns the lengths of each dimension with respect to their index. In MATLAB the first dimension (0 index in the
     * returned integer array) is the row length. The second dimension is the column length. The third dimension and
     * beyond are pages. The length of the returned array will be the number of dimensions returned by
     * {@link #dimensions()}.
     * 
     * @return lengths
     */
    public int[] lengths()
    {
        int[] lengthsCopy = new int[_lengths.length];
        System.arraycopy(_lengths, 0, lengthsCopy, 0, _lengths.length);
        
        return lengthsCopy;
    }
    
    /**
     * The number of dimensions this array has. This value will be the same as the length of the array returned by
     * {@link #lengths()}.
     * 
     * @return number of dimensions
     */
    public int dimensions()
    {
        return _lengths.length;
    }
    
    /**
     * Gets the value at {@code index} treating this array as the underlying one dimensional array. This  is equivalent
     * to indexing into a MATLAB array with just one subscript.
     * 
     * @param index
     * @return value at {@code index}
     * @throws ArrayIndexOutOfBoundsException if the linear index is out of bounds
     */
    public ComplexNumber get(int index)
    {
        return _supplier.create(_real, _imag, index);
    }
    
    /**
     * Gets the value at the specified {@code indices}.
     * 
     * @param indices
     * @return value at {@code indices}
     * @throws IllegalArgumentException if number of indices does not equal the number of dimensions
     * @throws ArrayIndexOutOfBoundsException if the indices are out of bound
     */
    public ComplexNumber get(int... indices)
    {
        if(indices.length == this.dimensions())
        {
            //Check the indices are in bounds
            for(int i = 0; i < indices.length; i++)
            {
                if(indices[i] >= _lengths[i])
                {
                    throw new ArrayIndexOutOfBoundsException("[" + indices[i] + "] is out of bounds for dimension " +
                            i + " where the length is " + _lengths[i]);
                }
            }
        }
        else
        {   
            throw new IllegalArgumentException("Array has " + this.dimensions() + " dimension(s), it cannot be " + 
                    "used indexed into using " + indices.length + " indices");
        }
        
        
        int linearIndex = ArrayTransformUtils.multidimensionalIndicesToLinearIndex(_lengths, indices);
        
        return _supplier.create(_real, _imag, linearIndex);
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
                " type=" + _real.getClass().getComponentType().getCanonicalName() + "," +
                " dimensions=" + this.dimensions() + "," +
                " size=" + this.size() + "," +
                " lengths=" + Arrays.toString(_lengths) + "]";
    }

    @Override
    MatlabTypeSetter getSetter()
    {
        return new MatlabNumericArraySetter(_real, _imag, _lengths);
    }
    
    static class MatlabNumericArrayGetter implements MatlabTypeGetter
    {
        private Object _real;
        private Object _imag;
        private int[] _lengths;
        private boolean _retreived = false;
        
        @Override
        public MatlabNumericArray retrieve()
        {
            if(!_retreived)
            {
                throw new IllegalStateException("array has not been retrieved");
            }
            
            return new MatlabNumericArray(_real, _imag, _lengths);
        }

        @Override
        public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            PrimitiveArrayGetter realGetter = new PrimitiveArrayGetter(true, true);
            realGetter.getInMatlab(ops, variableName);
            _real = realGetter.retrieve();
            _lengths = realGetter.getLengths();
            
            PrimitiveArrayGetter imagGetter = new PrimitiveArrayGetter(false, true);
            imagGetter.getInMatlab(ops, variableName);
            _imag = imagGetter.retrieve();
            
            _retreived = true;
        }
    }
    
    private class MatlabNumericArraySetter implements MatlabTypeSetter
    {
        private final Object _real;
        private final Object _imag;
        private final int[] _lengths;
        
        public MatlabNumericArraySetter(Object real, Object imag, int[] lengths)
        {
            _real = real;
            _imag = imag;
            _lengths = lengths;
        }
        
        @Override
        public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            ops.setVariable(variableName, this);
            
            String command = variableName + " = reshape(" + variableName + ".getReal()";
            if(_imag != null)
            {
                command += " + " + variableName + ".getImaginary() * i";
            }
            
            if(_lengths.length == 1)
            {   
                command += ", 1, " + _lengths[0] + ");";
            }
            else
            {
                for(int length : _lengths)
                {
                    command += ", " + length;
                }
                command += ");";
            }

            ops.eval(command);
        }
        
        public Object getReal()
        {
            return _real;
        }
        
        public Object getImaginary()
        {
            return _imag;
        }
    }
    
    private static interface ComplexSupplier<A>
    {
        public ComplexNumber create(A real, A imag, int index);
    }
    
    private static final Map<Class<?>, ComplexSupplier> COMPLEX_SUPPLIERS;
    static
    {
        Map<Class<?>, ComplexSupplier> map = new HashMap<Class<?>, ComplexSupplier>();
        
        map.put(byte.class, new ComplexByteSupplier());
        map.put(short.class, new ComplexShortSupplier());
        map.put(int.class, new ComplexIntegerSupplier());
        map.put(long.class, new ComplexLongSupplier());
        map.put(float.class, new ComplexFloatSupplier());
        map.put(double.class, new ComplexDoubleSupplier());
        
        COMPLEX_SUPPLIERS = Collections.unmodifiableMap(map);
    }
    
    private static class ComplexByteSupplier implements ComplexSupplier<byte[]>
    {
        @Override
        public ComplexByte create(byte[] real, byte[] imag, int index)
        {
            return new ComplexByte(real[index], (imag == null ? 0 : imag[index]));
        }
    }
    
    private static class ComplexShortSupplier implements ComplexSupplier<short[]>
    {
        @Override
        public ComplexShort create(short[] real, short[] imag, int index)
        {
            return new ComplexShort(real[index], (imag == null ? 0 : imag[index]));
        }
    }
    
    private static class ComplexIntegerSupplier implements ComplexSupplier<int[]>
    {
        @Override
        public ComplexInteger create(int[] real, int[] imag, int index)
        {
            return new ComplexInteger(real[index], (imag == null ? 0 : imag[index]));
        }
    }
    
    private static class ComplexLongSupplier implements ComplexSupplier<long[]>
    {
        @Override
        public ComplexLong create(long[] real, long[] imag, int index)
        {
            return new ComplexLong(real[index], (imag == null ? 0 : imag[index]));
        }
    }
    
    private static class ComplexFloatSupplier implements ComplexSupplier<float[]>
    {
        @Override
        public ComplexFloat create(float[] real, float[] imag, int index)
        {
            return new ComplexFloat(real[index], (imag == null ? 0 : imag[index]));
        }
    }
    
    private static class ComplexDoubleSupplier implements ComplexSupplier<double[]>
    {
        @Override
        public ComplexDouble create(double[] real, double[] imag, int index)
        {
            return new ComplexDouble(real[index], (imag == null ? 0 : imag[index]));
        }
    }
}