/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

/**
 * Represents a failure to connect to MATLAB or make MATLAB available for a connection.
 * 
 * @since 2.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabConnectionException extends Exception {
	private static final long serialVersionUID = 0xA400L;

	MatlabConnectionException(String msg) {
		super(msg);
	}

	MatlabConnectionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
