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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Information about a MATLAB function.
 *
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MatlabFunctionInfo
{   
    /**
     * The name of a MATLAB function which is on MATLAB's {@code path}. If this element is specified, then the
     * {@code path} element cannot also be specified.
     * 
     * @return 
     */
    String name() default "";
    
    /**
     * The fully qualified path to the MATLAB function's m-file. If this element is specified, then the {@code name}
     * element cannot also be specified.
     * 
     * @return 
     */
    String path() default "";
    
    /**
     * If the specified {@code path} element is relative to the interface. This allows for bundling m-files inside of
     * jars.
     * 
     * @return 
     */
    boolean isRelativePath() default false;
    
    /**
     * The number of arguments to be returned from the function. This element does not need to be set, but if it is not
     * then the method must have a {@code void} return type. This element may not be negative.
     * 
     * @return 
     */
    int nargout() default 0;
    
    /**
     * Whether the function should be invoked using {@code eval}. This element does not to need to be set, by default it
     * is set to {@code false}. 
     * 
     * @return 
     */
    boolean eval() default false;
}