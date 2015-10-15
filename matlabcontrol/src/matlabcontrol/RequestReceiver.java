/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Represents a receiver for a request to create a proxy. The receiver must be bound to an RMI registry, it will be
 * bound with the RMI identifier {@link #getReceiverID()}. Necessary to have this interface for RMI.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface RequestReceiver extends Remote {
	/**
	 * Receives an incoming wrapper around the JMI functionality inside of MATLAB.
	 * <br><br>
	 * This method is to be called by {@link MatlabConnector} running inside of MATLAB's JVM.
	 * 
	 * @param jmiWrapper
	 * @param existingSession if the session sending the jmiWrapper was running prior to the request to create the proxy
	 * @throws RemoteException 
	 */
	public void receiveJMIWrapper(JMIWrapperRemote jmiWrapper, boolean existingSession) throws RemoteException;

	/**
	 * The identifier of the receiver.
	 * 
	 * @return
	 * @throws RemoteException 
	 */
	public String getReceiverID() throws RemoteException;

	/**
	 * The classpath of the VM the receiver was created in encoded as an RMI codebase.
	 * 
	 * @return
	 * @throws RemoteException 
	 */
	public String getClassPathAsRMICodebase() throws RemoteException;

	/**
	 * The classpath of the VM the receiver was created in encoded as canonical paths.
	 * 
	 * @return
	 * @throws RemoteException 
	 */
	public String[] getClassPathAsCanonicalPaths() throws RemoteException;
}
