/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

/**
 * Represents a failure to invoke a method on the MATLAB session.
 * 
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabInvocationException extends Exception {
	private static final long serialVersionUID = 0xB400L;

	static enum Reason {
		INTERRRUPTED("Method could not be completed because the MATLAB thread was interrupted before MATLAB returned"), PROXY_NOT_CONNECTED("The proxy is not connected to MATLAB"), UNMARSHAL("Object attempting to be received cannot be transferred between Java Virtual Machines"), MARSHAL("Object attempting to be sent cannot be transferred between Java Virtual Machines"), INTERNAL_EXCEPTION("Method did not return properly because of an internal MATLAB exception"), NARGOUT_MISMATCH("Number of arguments returned did not match excepted"), EVENT_DISPATCH_THREAD("Issue pumping Event Dispatch Thread"), RUNTIME_EXCEPTION("RuntimeException occurred in MatlabThreadCallable, see cause for more information"), UNKNOWN("Method could not be invoked for an unknown reason, see cause for more information");

		private final String _message;

		private Reason(String msg) {
			_message = msg;
		}

		MatlabInvocationException asException() {
			return new MatlabInvocationException(_message);
		}

		MatlabInvocationException asException(Throwable cause) {
			return new MatlabInvocationException(_message, cause);
		}

		MatlabInvocationException asException(String additionalInfo) {
			return new MatlabInvocationException(_message + ": " + additionalInfo);
		}

		MatlabInvocationException asException(String additionalInfo, Throwable cause) {
			return new MatlabInvocationException(_message + ": " + additionalInfo, cause);
		}
	}

	private MatlabInvocationException(String msg) {
		super(msg);
	}

	private MatlabInvocationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
