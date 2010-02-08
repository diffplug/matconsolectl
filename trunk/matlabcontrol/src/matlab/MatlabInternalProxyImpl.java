package matlab;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Passes method calls off to the MatlabControl. This proxy is necessary because
 * fields that MatlabControl uses cannot be marshalled, as is required by RMI.
 * 
 * These methods are documented in MatlabProxy.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabInternalProxyImpl extends UnicastRemoteObject implements MatlabInternalProxy
{
	private static final long serialVersionUID = 1L;
	
	private MatlabController _control;
	
	public MatlabInternalProxyImpl() throws java.rmi.RemoteException
	{
		_control = new MatlabController();
	}

	public Object blockingFeval(String command, Object[] args) throws RemoteException, InterruptedException
	{
		return _control.blockingFeval(command, args);
	}

	public void eval(String command) throws RemoteException
	{
		_control.eval(command);
	}

	public void feval(String command, Object[] args) throws RemoteException
	{
		_control.feval(command, args);
	}

	public void setEchoEval(boolean echo) throws RemoteException
	{
		_control.setEchoEval(echo);
	}
	
	public void checkConnection() throws RemoteException { }
}