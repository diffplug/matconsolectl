/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.MatlabProxy;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class LinkedOperations implements MatlabOperations {
	private final MatlabProxy _delegateProxy;
	private final MatlabOperations _delegateOperations;

	public LinkedOperations(MatlabProxy proxy) {
		_delegateProxy = proxy;
		_delegateOperations = Linker.getLinkedMatlabOperations(proxy);
	}

	@Override
	public void eval(String command) throws MatlabInvocationException {
		_delegateOperations.eval(command);
	}

	@Override
	public Object[] returningEval(String command, int nargout) throws MatlabInvocationException {
		return _delegateOperations.returningEval(command, nargout);
	}

	@Override
	public void feval(String functionName, Object... args) throws MatlabInvocationException {
		_delegateOperations.feval(functionName, args);
	}

	@Override
	public Object[] returningFeval(String functionName, int nargout, Object... args) throws MatlabInvocationException {
		return _delegateOperations.returningFeval(functionName, nargout, args);
	}

	@Override
	public void setVariable(String variableName, Object value) throws MatlabInvocationException {
		_delegateOperations.setVariable(variableName, value);
	}

	@Override
	public Object getVariable(String variableName) throws MatlabInvocationException {
		return _delegateOperations.getVariable(variableName);
	}

	/**
	 * The proxy used to communicate with MATLAB.
	 * 
	 * @return proxy
	 */
	public MatlabProxy getDelegateProxy() {
		return _delegateProxy;
	}
}
