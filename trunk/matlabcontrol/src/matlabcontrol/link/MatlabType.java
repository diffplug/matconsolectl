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

import java.io.Serializable;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Hidden superclass of all Java classes which convert between MATLAB types. Subclasses are all final and are not
 * {@link Serializable}. Being final makes it easier to ensure appropriate behavior when transforming types
 * automatically. They are not serializable to reduce the publicly exposed API and reduce the need to maintain
 * serializable compatibility. Instead, transferring occurs by use of {@link MatlabTypeSetter} and
 * {@link MatlabTypeGetter}. A getter is typically associated with a class as inner static class.
 * 
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabType
{   
    abstract MatlabTypeSetter getSetter();
       
    /**
     * Retrieves in MATLAB the information necessary to create the associated {@code MatlabType} from a given MATLAB
     * variable.
     * <br><br>
     * Must have an accessible no argument constructor.
     * 
     * @param <U> 
     */
    static interface MatlabTypeGetter extends Serializable
    {
        /**
         * Takes the information retrieved by the
         * {@link #getInMatlab(matlabcontrol.MatlabOperations, java.lang.String)} and creates the
         * associated {@code MatlabType}.
         * 
         * @return 
         */
        public Object retrieve();
        
        /**
         * Retrieves the data it needs from the variable in MATLAB. So that after retrieving this information
         * {@link #retrieve()} can be called to create the appropriate {@code MatlabType}.
         * 
         * @param ops
         * @param variableName 
         */
        public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException;
    }
    
    /**
     * Sets in MATLAB the equivalent of the data represented by the {@code MatlabType} that provides an instance of
     * an implementation of this class.
     */
    static interface MatlabTypeSetter extends Serializable
    {
        public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException;
    }
}