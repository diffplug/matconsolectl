/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package com.mathworks.jmi;

/**
 * A partial stubbed out implementation of the Matlab class with the method's used by matlabcontrol. This stub exists
 * so that matlabcontrol can compile against this library. At runtime this library is not referenced, instead the
 * jmi.jar on MATLAB's classpath is used. The build script for matlabcontrol intentionally does not include this
 * library in the distribution folder it generates.
 *
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class Matlab {
	public static Object mtFevalConsoleOutput(String function, Object[] args, int nargout) throws Exception {
		throw new UnsupportedOperationException("stub");
	}

	public static void whenMatlabIdle(Runnable runnable) {
		throw new UnsupportedOperationException("stub");
	}
}
