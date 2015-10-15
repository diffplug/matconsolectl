/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.io.Serializable;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;

/**
 * Superclass of all Java classes which represent MATLAB types.
 * <br><br>
 * Subclasses of this either final or abstract and non-extenable with final subclasses of their own. Being final makes
 * it easier to ensure appropriate behavior when transforming types automatically. Subclasses are not
 * {@link Serializable} to reduce the publicly exposed API and reduce the need to maintain serializable compatibility.
 * Instead, transferring occurs by use of {@link MatlabTypeSetter} and {@link MatlabTypeGetter}. A getter is typically
 * associated with a class as an inner static class.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
abstract class MatlabType {
	MatlabType() {}

	abstract MatlabTypeSetter getSetter();

	/**
	 * Retrieves in MATLAB the information necessary to create the associated {@code MatlabType} from a given MATLAB
	 * variable.
	 * <br><br>
	 * Must have an accessible no argument constructor.
	 * 
	 * @param <U> 
	 */
	static interface MatlabTypeGetter<T> extends Serializable {
		/**
		 * Takes the information retrieved by the
		 * {@link #getInMatlab(matlabcontrol.MatlabOperations, java.lang.String)} and creates the
		 * associated {@code MatlabType}.
		 * 
		 * @return 
		 */
		public T retrieve();

		/**
		 * Retrieves the data it needs from the variable in MATLAB. So that after retrieving this information
		 * {@link #retrieve()} can be called to create the appropriate {@code MatlabType}.
		 * 
		 * @param ops
		 * @param variableName 
		 */
		public void getInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException;
	}

	/**
	 * Sets in MATLAB the equivalent of the data represented by the {@code MatlabType} that provides an instance of
	 * an implementation of this class.
	 */
	static interface MatlabTypeSetter extends Serializable {
		public void setInMatlab(MatlabOperations ops, String variableName) throws MatlabInvocationException;
	}
}
