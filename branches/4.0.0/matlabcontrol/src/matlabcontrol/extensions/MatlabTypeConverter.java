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

import java.io.Serializable;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

/**
 * Converts between MATLAB and Java types. Currently only supports numeric arrays.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabTypeConverter
{
    private final MatlabProxy _proxy;
    
    /**
     * Constructs the converter.
     * 
     * @param proxy
     */
    public MatlabTypeConverter(MatlabProxy proxy)
    {
        _proxy = proxy;
    }
    
    /**
     * Retrieves the MATLAB numeric array with the variable name {@code arrayName}.
     * 
     * @param arrayName
     * @return the retrieved numeric array
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    public MatlabNumericArray getNumericArray(String arrayName) throws MatlabInvocationException
    {
        ArrayInfo info = _proxy.invokeAndWait(new GetArrayCallable(arrayName));
        
        return new MatlabNumericArray(info.real, info.imaginary, info.lengths);
    }
    
    private static class GetArrayCallable implements MatlabThreadCallable<ArrayInfo>, Serializable
    {
        private final String _arrayName;
        
        public GetArrayCallable(String arrayName)
        {
            _arrayName = arrayName;
        }

        @Override
        public ArrayInfo call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            //Retrieve real values
            Object realObject = proxy.returningEval("real(" + _arrayName + ");", 1)[0];
            double[] realValues = (double[]) realObject;
            
            //Retrieve imaginary values if present
            boolean isReal = ((boolean[]) proxy.returningEval("isreal(" + _arrayName + ");", 1)[0])[0];
            double[] imaginaryValues = null;
            if(!isReal)
            {
                Object imaginaryObject = proxy.returningEval("imag(" + _arrayName + ");", 1);
                imaginaryValues = (double[]) imaginaryObject;
            }

            //Retrieve lengths of array
            double[] size = (double[]) proxy.returningEval("size(" + _arrayName + ");", 1)[0];
            int[] lengths = new int[size.length];
            for(int i = 0; i < size.length; i++)
            {
                lengths[i] = (int) size[i];
            }

            return new ArrayInfo(realValues, imaginaryValues, lengths);
        }
    }
    
    private static class ArrayInfo implements Serializable
    {
        private final double[] real, imaginary;
        private final int[] lengths;
        
        public ArrayInfo(double[] real, double[] imaginary, int[] lengths)
        {
            this.real = real;
            this.imaginary = imaginary;
            this.lengths = lengths;
        }
    }
    
    /**
     * Stores the {@code array} in MATLAB with the variable name {@code arrayName}.
     * 
     * @param arrayName the variable name
     * @param array
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    public void setNumericArray(String arrayName, MatlabNumericArray array) throws MatlabInvocationException
    {
        _proxy.invokeAndWait(new SetArrayCallable(arrayName, array));
    }
    
    private static class SetArrayCallable implements MatlabThreadCallable<Object>, Serializable
    {
        private final String _arrayName;
        private final double[] _realArray, _imaginaryArray;
        private final int[] _lengths;
        
        private SetArrayCallable(String arrayName, MatlabNumericArray array)
        {
            _arrayName = arrayName;
            _realArray = array.getRealLinearArray();
            _imaginaryArray = array.getImaginaryLinearArray();
            _lengths = array.getLengths();
        }
        
        @Override
        public Object call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            //Store real array in the MATLAB environment
            String realArray = (String) proxy.returningEval("genvarname('" + _arrayName + "_real', who);", 1)[0];
            proxy.setVariable(realArray, _realArray);
            
            //If present, store the imaginary array in the MATLAB environment
            String imagArray = null;
            if(_imaginaryArray != null)
            {
                imagArray = (String) proxy.returningEval("genvarname('" + _arrayName + "_imag', who);", 1)[0];
                proxy.setVariable(imagArray, _imaginaryArray);
            }

            //Build a statement to eval
            // - If imaginary array exists, combine the real and imaginary arrays
            // - Set the proper dimension length metadata
            // - Store as arrayName
            String evalStatement = _arrayName + " = reshape(" + realArray;
            if(_imaginaryArray != null)
            {
                evalStatement += " + " + imagArray + " * i";
            }
            for(int length : _lengths)
            {
                evalStatement += ", " + length;
            }
            evalStatement += ");";
            proxy.eval(evalStatement);
            
            //Clear variables holding separate real and imaginary arrays
            proxy.eval("clear " + realArray + ";");
            proxy.eval("clear " + imagArray + ";");
            
            return null;
        }
    }
    
    /**
     * Returns a brief description of this converter. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " proxy=" + _proxy + "]";
    }
}