/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * Issue linking a Java method to a MATLAB function.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class LinkingException extends RuntimeException {
	private static final long serialVersionUID = 0xD500L;

	LinkingException(String msg) {
		super(msg);
	}

	LinkingException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
