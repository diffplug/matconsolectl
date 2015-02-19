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

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Represents a variable in MATLAB. The representation is not associated with a given session of MATLAB. An instance of
 * this class with a given name does not mean that a variable with that name exists in any session of MATLAB.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabVariable extends MatlabType
{
    private final String _name;

    /**
     * Constructs a representation of a MATLAB variable with the name specified by {@code name}.
     * 
     * @param name 
     * @throws IllegalArgumentException if {@code name} is not a valid MATLAB variable name
     */
    public MatlabVariable(String name)
    {
        //Validate variable name
        if(name.isEmpty())
        {
            throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
        }
        char[] nameChars = name.toCharArray();
        if(!Character.isLetter(nameChars[0]))
        {
            throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
        }
        for(char element : nameChars)
        {
            if(!(Character.isLetter(element) || Character.isDigit(element) || element == '_'))
            {
                throw new IllegalArgumentException("Invalid MATLAB variable name: " + name);
            }
        }
        _name = name;
    }

    /**
     * The name of this variable.
     * 
     * @return 
     */
    public String getName()
    {
        return _name;
    }
        
    /**
     * Returns a brief description of this variable. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " name=" + _name + "]";
    }
    
    /**
     * Returns {@code true} if and only if {@code obj} is a {@code MatlabVariable} and has the same name as this
     * variable.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean equal = false;
        
        if(obj instanceof MatlabVariable)
        {
            MatlabVariable other = (MatlabVariable) obj;
            equal = other._name.equals(_name);
        }
        
        return equal;
    }
    
    /**
     * Returns a hash code consistent with {@link #equals(java.lang.Object)}.
     * 
     * @return 
     */
    @Override
    public int hashCode()
    {
        return _name.hashCode();
    }
    
    static class MatlabVariableGetter implements MatlabTypeGetter
    {
        private static final long serialVersionUID = 7724337165919355824L;
        private String _name;
        private boolean _retrieved;
        
        @Override
        public MatlabVariable retrieve()
        {
            if(!_retrieved)
            {
                throw new IllegalStateException("variable not retrieved");
            }
            
            return new MatlabVariable(_name);
        }

        @Override
        public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            _name = variableName;
            _retrieved = true;
        }
    }

    @Override
    MatlabTypeSetter getSetter()
    {
        return new MatlabVariableSetter(_name);
    }

    private static class MatlabVariableSetter implements MatlabTypeSetter
    {
        private static final long serialVersionUID = -2208485477826441076L;
        private final String _name;

        private MatlabVariableSetter(String name)
        {
            _name = name;
        }

        @Override
        public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException
        {
            ops.eval(variableName + " = " + _name + ";");
        }
    }
}