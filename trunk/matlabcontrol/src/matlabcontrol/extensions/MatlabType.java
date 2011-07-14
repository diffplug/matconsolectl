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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

/**
 * Hidden superclass of all Java classes which convert between MATLAB types. Subclasses are all final and are not
 * {@link Serializable}. Being final makes it easier to ensure appropriate behavior when transforming types
 * automatically. They are not serializable to reduce the publicly exposed API and reduce the need to maintain
 * serializable compatibility. Instead, transferring occurs by use of {@link MatlabTypeSerializedSetter} and
 * {@link MatlabTypeSerializedGetter}. A getter is associated with a class, not an instance of a class. As such it
 * cannot be retrieved in a generalizable manner with a method of the class. Instead if a class can be retrieved from
 * MATLAB as a MatlabType then the class is annotated with a {@link MatlabTypeSerializationProvider} that specifies
 * which class is its {@link MatlabType.MatlabTypeSerializedGetter}. The getter must have an accessible no argument
 * constructor.
 * <br><br>
 * This class is hidden for a few reasons. {@link MatlabFunctionLinker} in a variety of situations needs to determine if
 * something is a subclass of {@code MatlabType}, but it would not work if the type was actually a {@code MatlabType}.
 * Allowing it as either a parameter type or return type would complicate the logic, and in the case of a return type
 * or annotated return type would not in all situations work. It would generally degrade type safety to allow it. This
 * could be disallowed by checking for it. But the easiest way is to just not expose it. Furthermore, hiding this class
 * makes it easier to change the underlying structure without breaking public API binary compatibility.
 * 
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabType<T extends MatlabType>
{
    /**
     * All {@code MatlabType} subclasses that can return information from MATLAB (typically all types, but for instance
     * not {@link MatlabVariable}) must be annotated with this.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface MatlabTypeSerializationProvider
    {
        Class<? extends MatlabTypeSerializedGetter> value();
    }
    
    abstract MatlabTypeSerializedSetter<T> getSerializedSetter();
    
    /**
     * Returns a new instance of the {@link MatlabTypeSerializedGetter} associated with the {@link MatlabType} subclass.
     * Uses the getter that {@code clazz} specifies in its {@link MatlabTypeSerializationProvider} annotation.
     * 
     * @param <U>
     * @param clazz
     * @return 
     * @throws IllegalArgumentException if a getter cannot be created for {@code clazz}
     */
    static <U extends MatlabType> MatlabTypeSerializedGetter<U> newSerializedGetter(Class<U> clazz)
    {
        try
        {
            MatlabTypeSerializationProvider provider = clazz.getAnnotation(MatlabTypeSerializationProvider.class);
            Class<? extends MatlabTypeSerializedGetter> getterClass = provider.value();
            
            return getterClass.newInstance();
        }
        catch(Exception ex)
        {
            throw new IllegalArgumentException("Unable to create serialized getter for " + clazz.getName(), ex);
        }
    }
    
    /**
     * Retrieves in MATLAB the information necessary to create the associated {@code MatlabType} from a given MATLAB
     * variable.
     * <br><br>
     * Must have an accessible no argument constructor.
     * 
     * @param <U> 
     */
    static interface MatlabTypeSerializedGetter<U extends MatlabType> extends Serializable
    {
        /**
         * Takes the information retrieved by the
         * {@link #getInMatlab(matlabcontrol.MatlabProxy.MatlabThreadProxy, java.lang.String)} and creates the
         * associated {@code MatlabType}.
         * 
         * @return 
         */
        public U deserialize();
        
        /**
         * Retrieves the data it needs from the variable in MATLAB. So that after retrieving this information
         * {@link #deserialize()} can be called to create the appropriate {@code MatlabType}.
         * 
         * @param proxy
         * @param variableName 
         */
        public void getInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException;
    }
    
    /**
     * Sets in MATLAB the equivalent of the data represented by the {@code MatlabType} that provides an instance of
     * an implementation of this class.
     * 
     * @param <U> 
     */
    static interface MatlabTypeSerializedSetter<U extends MatlabType> extends Serializable
    {
        public void setInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException;
    }
}