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
     * Either the name of a MATLAB function or a path to a MATLAB function. The value provided to this element is
     * resolved in the following order:
     * <ol>
     * <li><b>Valid MATLAB function name</b><br/>
     *     The value will be treated as a function on MATLAB's path. Whether a function with the specified name is
     *     actually on MATLAB's path will not be confirmed.</li>
     * <li><b>Absolute path to an m-file</b><br/>
     *     The value will be treated as the location of an m-file. The file's existence will be confirmed.</li>
     * <li><b>Relative path to an m-file</b><br/>
     *     The value will be treated as the location of an m-file relative to the root directory of the interface which
     *     declared the method being annotated. For example if the interface is {@code com.example.MyInterface} located
     *     at {@code /projects/code/numera/com/example/MyInterface.java} then path will be resolved relative to
     *     {@code /projects/code/numera/}. This path can be resolved properly when both the interface and m-file are
     *     inside of a jar. The file's existence will be confirmed.</li>
     * </ol>
     * The validity of this element's value will be determined when the interface containing the method being annotated
     * is provided to {@link MatlabFunctionLinker#link(java.lang.Class, matlabcontrol.MatlabProxy)}.
     * 
     * @return 
     */
    String value();
}