/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.rmi.registry.Registry;
import java.util.UUID;

/**
 * Implementation of {@link MatlabSession}. Split into interface and implementation to work properly with RMI.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabSessionImpl implements MatlabSession {
	/**
	 * The prefix for all RMI names of bound instances of {@link MatlabSession}.
	 */
	private static final String MATLAB_SESSION_PREFIX = "MATLAB_SESSION_";

	/**
	 * The bound name in the RMI registry for this instance.
	 */
	private final String SESSION_ID = MATLAB_SESSION_PREFIX + UUID.randomUUID().toString();

	@Override
	public synchronized boolean connectFromRMI(String receiverID, int port) {
		boolean success = false;
		if (MatlabConnector.isAvailableForConnection()) {
			MatlabConnector.connect(receiverID, port, true);
			success = true;
		}

		return success;
	}

	/**
	 * The unique identifier for this session.
	 * 
	 * @return 
	 */
	String getSessionID() {
		return SESSION_ID;
	}

	/**
	 * Attempts to connect to a running instance of MATLAB. Returns {@code true} if a connection was made,
	 * {@code false} otherwise.
	 * 
	 * @param receiverID
	 * @param port
	 * @return if connection was made
	 */
	static boolean connectToRunningSession(String receiverID, int port) {
		boolean establishedConnection = false;

		try {
			Registry registry = LocalHostRMIHelper.getRegistry(port);

			String[] remoteNames = registry.list();
			for (String name : remoteNames) {
				if (name.startsWith(MATLAB_SESSION_PREFIX)) {
					MatlabSession session = (MatlabSession) registry.lookup(name);
					if (session.connectFromRMI(receiverID, port)) {
						establishedConnection = true;
						break;
					}
				}
			}
		} catch (Exception e) {}

		return establishedConnection;
	}
}
