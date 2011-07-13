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
import matlabcontrol.extensions.MatlabType.MatlabTypeSerializedGetter;
import matlabcontrol.extensions.MatlabType.MatlabTypeSerializedSetter;

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
     * Retrieves the MATLAB numeric array with the variable name {@code variableName}.
     * 
     * @param variableName
     * @return the retrieved numeric array
     * @throws MatlabInvocationException if thrown by the proxy
     * @throws IncompatibleReturnException if the variable specified by {@code arrayName} is not a MATLAB numeric array
     */
    public MatlabNumericArray getNumericArray(String variableName) throws MatlabInvocationException
    {
        return getMatlabType(MatlabNumericArray.class, variableName);
    }
    
    /**
     * Stores the {@code array} in MATLAB with the variable name {@code variableName}.
     * 
     * @param variableName the variable name
     * @param array
     * @throws MatlabInvocationException if thrown by the proxy
     */
    public void setNumericArray(String variableName, MatlabNumericArray array) throws MatlabInvocationException
    {
        setMatlabType(array, variableName);
    }
    
    public MatlabFunctionHandle getFunctionHandle(String variableName) throws MatlabInvocationException
    {
        return getMatlabType(MatlabFunctionHandle.class, variableName);
    }
    
    public void setFunctionHandle(String variableName, MatlabFunctionHandle handle) throws MatlabInvocationException
    {
        setMatlabType(handle, variableName);
    }
    
    
    
    private <T extends MatlabType> T getMatlabType(Class<T> type, String variableName) throws MatlabInvocationException
    {
        MatlabTypeSerializedGetter<T> getter = MatlabType.createSerializedGetter(type);
        getter = _proxy.invokeAndWait(new GetTypeCallable(getter, variableName));
        
        return getter.deserialize();
    }
    
    private static class GetTypeCallable implements MatlabThreadCallable<MatlabTypeSerializedGetter>, Serializable
    {
        private final MatlabType.MatlabTypeSerializedGetter _getter;
        private final String _variableName;
        
        private GetTypeCallable(MatlabTypeSerializedGetter getter, String variableName)
        {
            _getter = getter;
            _variableName = variableName;
        }

        @Override
        public MatlabTypeSerializedGetter call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            _getter.getInMatlab(proxy, _variableName);
            
            return _getter;
        }
    }
    
    private void setMatlabType(MatlabType type, String variableName) throws MatlabInvocationException
    {
        _proxy.invokeAndWait(new SetTypeCallable(type.getSerializedSetter(), variableName));
    }
    
    private static class SetTypeCallable implements MatlabThreadCallable<Void>, Serializable
    {
        private final MatlabTypeSerializedSetter _setter;
        private final String _variableName;
        
        private SetTypeCallable(MatlabTypeSerializedSetter setter, String variableName)
        {
            _setter = setter;
            _variableName = variableName;
        }

        @Override
        public Void call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            _setter.setInMatlab(proxy, _variableName);
            
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