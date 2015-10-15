/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

/**
 * A wrapper around any {@link Throwable} so that it  can be sent over RMI without needing the class to be defined in
 * the receiving JVM. The stack trace will print as if it were the original throwable.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ThrowableWrapper extends Throwable {
	private static final long serialVersionUID = 0xC500L;

	/**
	 * The {@code String} representation of the {@code MatlabException} so that this exception can pretend to be a
	 * {@code MatlabException}.
	 */
	private final String _toString;

	/**
	 * Creates a wrapper around {@code innerThrowable} so that when the stack trace is printed it is the same to the
	 * developer, but can be sent over RMI without the throwable being defined in the other JVM.
	 * 
	 * @param innerThrowable
	 */
	ThrowableWrapper(Throwable innerThrowable) {
		super(msgFromToString(innerThrowable));
		//Store innerThrowable's toString() value
		_toString = innerThrowable.toString();

		//Set this stack trace to that of the innerThrowable
		this.setStackTrace(innerThrowable.getStackTrace());

		//Store the cause, wrapping it
		if (innerThrowable.getCause() != null) {
			this.initCause(new ThrowableWrapper(innerThrowable.getCause()));
		}
	}

	/**
	 * The innerThrowable seems to not implement getMessage() properly.
	 * 
	 * So we extract the message from the exception's toString() method.
	 */
	static String msgFromToString(Throwable e) {
		String msg = e.toString();
		int idx = msg.indexOf(':');
		if (idx >= 0 && idx < msg.length() - 1) {
			return msg.substring(idx + 1);
		} else {
			return msg;
		}
	}

	@Override
	public String toString() {
		return _toString;
	}
}
