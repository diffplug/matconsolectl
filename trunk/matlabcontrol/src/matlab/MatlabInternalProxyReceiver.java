package matlab;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Implement this interface to receive a MatlabInternalProxy. Necessary to have
 * this interface for RMI.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
interface MatlabInternalProxyReceiver extends Remote
{
	public void registerControl(String bindValue, MatlabInternalProxy controller) throws RemoteException;
}