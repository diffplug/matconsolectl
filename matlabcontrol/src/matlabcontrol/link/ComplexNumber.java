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
import java.util.Map;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Corresponds to a MATLAB number with real and imaginary components.
 * 
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public abstract class ComplexNumber extends MatlabType
{   
    private final Number _real;
    private final Number _imag;
    
    ComplexNumber(Number real, Number imag)
    {
        _real = real;
        _imag = imag;
    }
    
    /**
     * Returns the real value as a {@code byte}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public byte byteValue()
    {
        return _real.byteValue();
    }
    
    /**
     * Returns the real value as a {@code short}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public short shortValue()
    {
        return _real.shortValue();
    }
        
    /**
     * Returns the real value as an {@code int}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public int intValue()
    {
        return _real.intValue();
    }
        
    /**
     * Returns the real value as a {@code long}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public long longValue()
    {
        return _real.longValue();
    }
    
    /**
     * Returns the real value as a {@code float}. This may involve rounding.
     * 
     * @return 
     */
    public float floatValue()
    {
        return _real.floatValue();
    }
        
    /**
     * Returns the real value as a {@code double}. This may involve rounding.
     * 
     * @return 
     */
    public double doubleValue()
    {
        return _real.doubleValue();
    }
    
    /**
     * Returns the imaginary value as a {@code byte}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public byte byteValueImaginary()
    {
        return _imag.byteValue();
    }
    
    /**
     * Returns the imaginary value as a {@code short}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public short shortValueImaginary()
    {
        return _imag.shortValue();
    }
        
    /**
     * Returns the imaginary value as an {@code int}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public int intValueImaginary()
    {
        return _imag.intValue();
    }
        
    /**
     * Returns the imaginary value as a {@code long}. This may involve rounding or truncation.
     * 
     * @return 
     */
    public long longValueImaginary()
    {
        return _imag.longValue();
    }
        
    /**
     * Returns the imaginary value as a {@code float}. This may involve rounding.
     * 
     * @return 
     */
    public float floatValueImaginary()
    {
        return _imag.floatValue();
    }
        
    /**
     * Returns the imaginary value as a {@code double}. This may involve rounding.
     * 
     * @return 
     */
    
    public double doubleValueImaginary()
    {
        return _imag.doubleValue();
    }

    @Override
    MatlabTypeSetter getSetter()
    {
        return new ComplexNumberSetter(_real, _imag);
    }
    
    //This is very non-object oriented to have this here instead of each subclass, but it saves a lot of boilerplate
    private static class ComplexNumberSetter implements MatlabTypeSetter
    {
        private final Number _real;
        private final Number _imag;
        
        private ComplexNumberSetter(Number real, Number imag)
        {
            _real = real;
            _imag = imag;
        }

        @Override
        public void setInMatlab(MatlabOperations ops, String name) throws MatlabInvocationException
        {
            //Avoids limitation that MATLAB cannot multiply an integer stored in a variable by i
                    
            if(_real instanceof Byte)
            {
                ops.eval(name + "=int8(" + _real.byteValue() + "+" + _imag.byteValue() + "i);");
            }
            else if(_real instanceof Short)
            {
                ops.eval(name + "=int16(" + _real.shortValue() + "+" + _imag.shortValue() + "i);");
            }
            else if(_real instanceof Integer)
            {
                ops.eval(name + "=int32(" + _real.intValue() + "+" + _imag.intValue() + "i);");
            }
            else if(_real instanceof Long)
            {
                ops.eval(name + "=int64(" + _real.longValue() + "+" + _imag.longValue() + "i);");
            }
                    
            // Set value through an array to avoid values being converted to MATLAB double
                    
            else if(_real instanceof Float)
            {
                ops.setVariable(name, new float[] { _real.floatValue(), _imag.floatValue() });
                ops.eval(name + "=" + name + "(1)+" + name + "(2)*i;");
            }
            else if(_real instanceof Double)
            {
                ops.setVariable(name, new double[] { _real.doubleValue(), _imag.doubleValue() });
                ops.eval(name + "=" + name + "(1)+" + name + "(2)*i;");
            }
        }
    }
    
    static class ComplexNumberGetter implements MatlabTypeGetter
    {
        private Object _real;
        private Object _imag;
        private boolean _retrieved;

        @Override
        public Object retrieve()
        {
            if(!_retrieved)
            {
                throw new IllegalStateException("complex number has not been retrieved");
            }
            
            return COMPLEX_CREATORS.get(_real.getClass()).create(_real, _imag);
        }

        @Override
        public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            String receiverName = (String) ops.returningEval("genvarname('complex_receiver_', who);", 1)[0];
            try
            {
                ops.setVariable(receiverName, this);
                ops.eval(receiverName + ".setReal(real(" + variableName + "));");
                ops.eval(receiverName + ".setImaginary(imag(" + variableName + "));");
            }
            finally
            {
                ops.eval("clear " + receiverName);
            }
            
            _retrieved = true;
        }
        
        public void setReal(Object real)
        {
            _real = real;
        }
        
        public void setImaginary(Object imag)
        {
            _imag = imag;
        }
        
        private static final Map<Class, ComplexCreator> COMPLEX_CREATORS = new HashMap<Class, ComplexCreator>();
        static
        {
            COMPLEX_CREATORS.put(Byte.class, new ComplexByteCreator());
            COMPLEX_CREATORS.put(Short.class, new ComplexShortCreator());
            COMPLEX_CREATORS.put(Integer.class, new ComplexIntegerCreator());
            COMPLEX_CREATORS.put(Long.class, new ComplexLongCreator());
            COMPLEX_CREATORS.put(Float.class, new ComplexFloatCreator());
            COMPLEX_CREATORS.put(Double.class, new ComplexDoubleCreator());
        }
        
        private static interface ComplexCreator<T>
        {
            public ComplexNumber create(T real, T imag);
        }
        
        private static class ComplexByteCreator implements ComplexCreator<Byte>
        {
            @Override
            public ComplexByte create(Byte real, Byte imag)
            {
                return new ComplexByte(real, imag);
            }
        }
        
        private static class ComplexShortCreator implements ComplexCreator<Short>
        {
            @Override
            public ComplexShort create(Short real, Short imag)
            {
                return new ComplexShort(real, imag);
            }
        }   
        
        private static class ComplexIntegerCreator implements ComplexCreator<Integer>
        {
            @Override
            public ComplexInteger create(Integer real, Integer imag)
            {
                return new ComplexInteger(real, imag);
            }
        }   
        
        private static class ComplexLongCreator implements ComplexCreator<Long>
        {
            @Override
            public ComplexLong create(Long real, Long imag)
            {
                return new ComplexLong(real, imag);
            }
        }
        
        private static class ComplexFloatCreator implements ComplexCreator<Float>
        {
            @Override
            public ComplexFloat create(Float real, Float imag)
            {
                return new ComplexFloat(real, imag);
            }
        }
        
        private static class ComplexDoubleCreator implements ComplexCreator<Double>
        {
            @Override
            public ComplexDouble create(Double real, Double imag)
            {
                return new ComplexDouble(real, imag);
            }
        }
    }
    
    
    /**
     * Returns a {@code String} representation of the format <code><i>real</i> + <i>imag</i>i</code> if the imaginary
     * component is positive and <code><i>real</i> - <i>imag</i>i</code> if the imaginary component is negative.
     * <br><br>
     * Examples:<br>
     * {@code 3 + 5i}<br>
     * {@code 5.0 - 1.17549435E-38i}
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        String fullStr;
        String realStr = _real.toString();
        String imagStr = _imag.toString();
        
        if(imagStr.startsWith("-"))
        {
            fullStr = realStr + " - " + imagStr.substring(1, imagStr.length()) + "i";
        }
        else
        {
            fullStr = realStr + " + " + imagStr + "i";
        }
        
        return fullStr;
    }
    
    /**
     * Compares this object to the specified object. The result is {@code true} if and only if the other object is of
     * the same class as this {@code ComplexNumber} implementation and its component real and imaginary parts are both
     * equal. The definition of equality for the component parts is defined by the {@code equals(...)} method of the
     * boxed numeric class of its components. For instance, for two {@code ComplexFloat} instances to be equivalent
     * both of their real components must be equivalent and both of their imaginary components must be equivalent by the
     * definition of {@link Float#equals(java.lang.Object)}.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean equal = false;
        
        if(obj != null && this.getClass().equals(obj.getClass()))
        {
            ComplexNumber other = (ComplexNumber) obj;
            equal = _real.equals(other._real) && _imag.equals(other._imag);
        }
        
        return equal;
    }
    
    /**
     * Returns a hash code for this {@code ComplexNumber}.
     * 
     * @return 
     */
    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 79 * hash + _real.hashCode();
        hash = 79 * hash + _imag.hashCode();
        
        return hash;
    }
}