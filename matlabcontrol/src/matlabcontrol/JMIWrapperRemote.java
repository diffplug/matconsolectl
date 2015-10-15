/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Methods that can be called to control MATLAB except for {@link #checkConnection()}.
 * <br><br>
 * All of these methods throw RemoteException. RemoteException will be thrown if something occurs to disrupt the
 * communication between this JVM and the one MATLAB is running in. For instance, closing MATLAB will terminate its
 * JVM and then all method calls on this proxy will throw exceptions.
 * <br><br>
 * For descriptions of what these methods do see the corresponding methods in {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface JMIWrapperRemote extends Remote {
	public void exit() throws RemoteException;

	public void setVariable(String variableName, Object value) throws RemoteException, MatlabInvocationException;

	public Object getVariable(String variableName) throws RemoteException, MatlabInvocationException;

	public void eval(String command) throws RemoteException, MatlabInvocationException;

	public Object[] returningEval(String command, int nargout) throws RemoteException, MatlabInvocationException;

	public void feval(String command, Object... args) throws RemoteException, MatlabInvocationException;

	public Object[] returningFeval(String command, int nargout, Object... args) throws RemoteException, MatlabInvocationException;

	public <U> U invokeAndWait(MatlabProxy.MatlabThreadCallable<U> callable) throws RemoteException, MatlabInvocationException;

	/**
	 * This method does nothing. It is used internally to check if a connection is still active via calling this method
	 * and seeing if it throws a {@code RemoteException} (if it does, the connection is no longer active).
	 * 
	 * @throws RemoteException
	 */
	public void checkConnection() throws RemoteException;
}
