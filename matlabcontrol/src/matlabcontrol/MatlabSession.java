/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An implementation of this interface is bound to a RMI registry, representing this session of MATLAB.
 *
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface MatlabSession extends Remote {
	/**
	 * Attempts a connection to this session of MATLAB. If this session is available for connection it will send a
	 * {@link JMIWrapperRemote} to the receiver and {@code true} will be returned. Otherwise {@code false} will be
	 * returned and no other action will be taken.
	 * 
	 * @param receiverID
	 * @param port
	 * @throws RemoteException
	 * @return if connection was established
	 */
	public boolean connectFromRMI(String receiverID, int port) throws RemoteException;
}
