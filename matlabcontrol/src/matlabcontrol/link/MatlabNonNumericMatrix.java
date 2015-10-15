/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

/**
 * 
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabNonNumericMatrix<L, T> extends MatlabMatrix<L, T> {
	/**
	 * Returns an array that holds the values from the MATLAB matrix. Each call returns a new copy which may be used in
	 * any manner; modifications to it will have no effect on this instance.
	 * 
	 * @return array
	 */
	public T toArray() {
		return getBaseArray().toRealArray();
	}
}
