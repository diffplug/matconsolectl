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
     * The name of a MATLAB function which is on MATLAB's {@code path}.
     * <br><br>
     * If this element is specified, then {@code absolutePath} and {@code relativePath} cannot also be specified.
     * 
     * @return 
     */
    String name() default "";
    
    /**
     * The absolute path to the MATLAB function's m-file.
     * <br><br>
     * If this element is specified, then {@code name} and {@code relativePath} cannot also be specified.
     * 
     * @return 
     */
    String absolutePath() default "";
    
    /**
     * The relative path to the MATLAB function's m-file. The path is relative to the root directory of the interface
     * which contains the annotated method. For example if the interface is {@code com.example.MyInterface} located
     * at {@code /projects/code/numera/com/example/MyInterface.java} then path will be resolved relative to
     * {@code /projects/code/numera/}. This path can be resolved properly when both the interface and m-file are inside
     * of a jar.
     * <br><br>
     * If this element is specified, then {@code name} and {@code absolutePath} cannot also be specified.
     * 
     * @return 
     */
    String relativePath() default "";
    
    /**
     * The types of the values to be returned. To have a function return 2 or more arguments this element must be used.
     * When 0 or 1 arguments are to be returned this element should not be used.
     * 
     * @return 
     */
    Class[] returnTypes() default {};
}