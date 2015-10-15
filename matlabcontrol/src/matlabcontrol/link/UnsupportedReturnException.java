/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * Thrown if a MATLAB type cannot be returned from MATLAB to Java because it is not supported.
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class UnsupportedReturnException extends RuntimeException {
	private static final long serialVersionUID = 0xF500L;

	UnsupportedReturnException(String msg) {
		super(msg);
	}
}
