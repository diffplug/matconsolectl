/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.rmi.RemoteException;

/**
 * Passes method calls off to {@link JMIWrapper}.
 * <br><br>
 * Methods called on this proxy will be performed inside of the JVM that created this object. This class is only created
 * inside of the MATLAB's JVM and so {@code JMIWrapper}'s calls will be able to communicate with MATLAB.
 * <br><br>
 * These methods are documented in {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class JMIWrapperRemoteImpl extends LocalHostRMIHelper.LocalHostRemoteObject implements JMIWrapperRemote {
	private static final long serialVersionUID = 6263244863419922018L;

	public JMIWrapperRemoteImpl() throws RemoteException {}

	@Override
	public void exit() {
		JMIWrapper.exit();
	}

	@Override
	public void eval(String command) throws MatlabInvocationException {
		JMIWrapper.eval(command);
	}

	@Override
	public Object[] returningEval(String command, int nargout) throws MatlabInvocationException {
		return JMIWrapper.returningEval(command, nargout);
	}

	@Override
	public void feval(String command, Object... args) throws MatlabInvocationException {
		JMIWrapper.feval(command, args);
	}

	@Override
	public Object[] returningFeval(String command, int nargout, Object... args) throws MatlabInvocationException {
		return JMIWrapper.returningFeval(command, nargout, args);
	}

	@Override
	public void setVariable(String variableName, Object value) throws MatlabInvocationException {
		JMIWrapper.setVariable(variableName, value);
	}

	@Override
	public Object getVariable(String variableName) throws MatlabInvocationException {
		return JMIWrapper.getVariable(variableName);
	}

	@Override
	public <T> T invokeAndWait(MatlabProxy.MatlabThreadCallable<T> callable) throws MatlabInvocationException {
		return JMIWrapper.invokeAndWait(callable);
	}

	@Override
	public void checkConnection() {}
}
