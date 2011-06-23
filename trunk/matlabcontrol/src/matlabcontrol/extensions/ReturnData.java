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

/**
 * Encapsulates the data returned from MATLAB.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class ReturnData
{
    private final Object _data;
    
    ReturnData(Object data)
    {
        _data = data;
    }
    
    /**
     * Returns the data as an {@code Object}.
     * 
     * @return 
     */
    public Object get()
    {
        return _data;
    }
    
    /**
     * Returns the data as type {@code E}.
     * 
     * @param <E>
     * @param clazz
     * @return
     * @throws ClassCastException if the cast cannot be performed
     */
    public <E> E getAs(Class<E> clazz)
    {
        return clazz.cast(_data);
    }
    
    /**
     * Returns the data as a {@code String}.
     * 
     * @return
     * @throws ClassCastException if the cast cannot be performed
     */
    public String getAsString()
    {
        return this.getAs(String.class);
    }
    
    /**
     * Returns the data as a {@code double} array.
     * 
     * @return
     * @throws ClassCastException if the cast cannot be performed
     */
    public double[] getAsDoubleArray()
    {
        return this.getAs(double[].class);
    }
    
    /**
     * Returns the data as an {@code Object} array.
     * 
     * @return
     * @throws ClassCastException if the cast cannot be performed
     */
    public Object[] getAsObjectArray()
    {
        return this.getAs(Object[].class);
    }
    
    /**
     * Returns the data as type {@code E}. If the cast cannot be performed {@code null} will be returned.
     * 
     * @param <E>
     * @param clazz
     * @return 
     */
    public <E> E getIf(Class<E> clazz)
    {
        E convertedData = null;
        if(clazz.isInstance(_data))
        {
            convertedData = clazz.cast(_data);
        }
        
        return convertedData;
    }
    
    /**
     * Returns the data as a {@code String}. If the cast cannot be performed {@code null} will be returned.
     * 
     * @return 
     */
    public String getIfString()
    {
        return this.getIf(String.class);
    }
    
    /**
     * Returns the data as a {@code double} array. If the cast cannot be performed {@code null} will be returned.
     * 
     * @return 
     */
    public double[] getIfDoubleArray()
    {
        return this.getIf(double[].class);
    }
    
    /**
     * Returns the data as an {@code Object} array. If the cast cannot be performed {@code null} will be returned.
     * 
     * @return 
     */
    public Object[] getIfObjectArray()
    {
        return this.getIf(Object[].class);
    }
}