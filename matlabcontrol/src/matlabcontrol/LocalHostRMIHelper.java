/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Handles creation of RMI objects, making sure they only operate on localhost.
 *
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalHostRMIHelper {
	private static final LocalHostRMISocketFactory SOCKET_FACTORY = new LocalHostRMISocketFactory();

	public static Registry getRegistry(int port) throws RemoteException {
		return LocateRegistry.getRegistry("localhost", port, SOCKET_FACTORY);
	}

	public static Registry createRegistry(int port) throws RemoteException {
		return LocateRegistry.createRegistry(port, SOCKET_FACTORY, SOCKET_FACTORY);
	}

	public static Remote exportObject(Remote object) throws RemoteException {
		return UnicastRemoteObject.exportObject(object, 0, SOCKET_FACTORY, SOCKET_FACTORY);
	}

	private static class LocalHostRMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable {
		private static final long serialVersionUID = 2973279795727940224L;

		@Override
		public Socket createSocket(String host, int port) throws IOException {
			return SocketFactory.getDefault().createSocket(InetAddress.getByName("localhost"), port);
		}

		@Override
		public ServerSocket createServerSocket(int port) throws IOException {
			return ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof LocalHostRMISocketFactory);
		}

		@Override
		public int hashCode() {
			return 5;
		}

		/**
		 * Overridden to provide a better name for the RMI RenewClean thread.
		 * 
		 * @return 
		 */
		@Override
		public String toString() {
			return "MLC localhost Socket Factory";
		}
	}

	static class LocalHostRemoteObject extends UnicastRemoteObject {
		private static final long serialVersionUID = -7160502485279570444L;

		LocalHostRemoteObject() throws RemoteException {
			super(0, SOCKET_FACTORY, SOCKET_FACTORY);
		}
	}
}
