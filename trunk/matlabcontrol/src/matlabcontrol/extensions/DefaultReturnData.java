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
 * 
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class DefaultReturnData implements ReturnData
{
    protected final Object _data;
    
    protected DefaultReturnData(Object data)
    {
        _data = data;
    }
    
    @Override
    public Object get()
    {
        return _data;
    }
    
    public <E> E getAs(Class<E> clazz) throws ClassCastException
    {
        return clazz.cast(_data);
    }
    
    public String getAsString() throws ClassCastException
    {
        return this.getAs(String.class);
    }
    
    public double[] getAsDoubleArray() throws ClassCastException
    {
        return this.getAs(double[].class);
    }
    
    public Object[] getAsObjectArray() throws ClassCastException
    {
        return this.getAs(Object[].class);
    }
    
    public <E> E getIf(Class<E> clazz)
    {
        E convertedData = null;
        if(clazz.isInstance(_data))
        {
            convertedData = clazz.cast(_data);
        }
        
        return convertedData;
    }
    
    public String getIfString()
    {
        return this.getIf(String.class);
    }
    
    public double[] getIfDoubleArray()
    {
        return this.getIf(double[].class);
    }
    
    public Object[] getIfObjectArray()
    {
        return this.getIf(Object[].class);
    }
}