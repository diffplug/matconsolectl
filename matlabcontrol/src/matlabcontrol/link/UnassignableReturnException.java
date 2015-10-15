/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * The type to be returned is not assignable to the return type. This exception is conceptually similar to a
 * {@link ClassCastException} except that it is thrown before the cast would ever occur. For primitive return types,
 * this is similar to a {@link NullPointerException} being thrown when the return value is {@code null} and therefore
 * cannot be un-boxed.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class UnassignableReturnException extends RuntimeException {
	private static final long serialVersionUID = 0xE500L;

	UnassignableReturnException(String msg) {
		super(msg);
	}
}
