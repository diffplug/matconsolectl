/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

/**
 * Allows for calling MATLAB from <b>inside</b> of MATLAB.
 * 
 * @since 3.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalMatlabProxy extends MatlabProxy {
	/**
	 * If connected to MATLAB.
	 * 
	 * This notion of connection exists to make it consistent with {@link RemoteMatlabProxy}, but is not actually
	 * necessary. Unless a user calls {@link #disconnect()} this proxy cannot become disconnected.
	 */
	private volatile boolean _isConnected = true;

	LocalMatlabProxy(Identifier id) {
		super(id, true);
	}

	@Override
	public boolean isRunningInsideMatlab() {
		return true;
	}

	@Override
	public boolean isConnected() {
		return _isConnected;
	}

	@Override
	public boolean disconnect() {
		_isConnected = false;

		//Notify listeners
		notifyDisconnectionListeners();

		return true;
	}

	// Methods which interact with MATLAB

	@Override
	public void exit() throws MatlabInvocationException {
		if (this.isConnected()) {
			JMIWrapper.exit();
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public void eval(String command) throws MatlabInvocationException {
		if (this.isConnected()) {
			JMIWrapper.eval(command);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public Object[] returningEval(String command, int nargout) throws MatlabInvocationException {
		if (this.isConnected()) {
			return JMIWrapper.returningEval(command, nargout);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public void feval(String functionName, Object... args) throws MatlabInvocationException {
		if (this.isConnected()) {
			JMIWrapper.feval(functionName, args);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public Object[] returningFeval(String functionName, int nargout, Object... args) throws MatlabInvocationException {
		if (this.isConnected()) {
			return JMIWrapper.returningFeval(functionName, nargout, args);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public void setVariable(String variableName, Object value) throws MatlabInvocationException {
		if (this.isConnected()) {
			JMIWrapper.setVariable(variableName, value);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public Object getVariable(String variableName) throws MatlabInvocationException {
		if (this.isConnected()) {
			return JMIWrapper.getVariable(variableName);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}

	@Override
	public <T> T invokeAndWait(MatlabThreadCallable<T> callable) throws MatlabInvocationException {
		if (this.isConnected()) {
			return JMIWrapper.invokeAndWait(callable);
		} else {
			throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
		}
	}
}
