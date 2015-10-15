/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Information about a MATLAB function.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MatlabFunction {
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
	 *     inside of a zip file such as a jar or war file. The file's existence will be confirmed.</li>
	 * </ol>
	 * The validity of this element's value will be determined when the interface containing the method being annotated
	 * is provided to {@link MatlabFunctionLinker#link(java.lang.Class, matlabcontrol.MatlabProxy)}.
	 * 
	 * @return 
	 */
	String value();
}
