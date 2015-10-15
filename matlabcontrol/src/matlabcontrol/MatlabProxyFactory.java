/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import matlabcontrol.MatlabProxy.Identifier;

/**
 * Creates instances of {@link MatlabProxy}. Any number of proxies may be created with a factory.
 * <br><br>
 * How the proxies will connect to a session of MATLAB depends on whether the factory is running inside or outside
 * MATLAB:
 * <br><br>
 * <i>Running inside MATLAB</i><br>
 * The proxy will connect to the session of MATLAB this factory is running in.
 * <br><br>
 * <i>Running outside MATLAB</i><br>
 * By default a new session of MATLAB will be started and connected to, but the factory may be configured via the
 * options provided to this factory to connect to a previously controlled session.
 * <br><br>
 * This class is unconditionally thread-safe. Any number of proxies may be created simultaneously.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyFactory implements ProxyFactory {
	private final ProxyFactory _delegateFactory;

	/**
	 * Constructs the factory using default options.
	 * 
	 * @throws MatlabConnectionException 
	 */
	public MatlabProxyFactory() {
		this(new MatlabProxyFactoryOptions.Builder().build());
	}

	/**
	 * Constructs the factory with the specified {@code options}. Depending on the whether the factory is running inside
	 * MATLAB or outside MATLAB will determine if a given option is used.
	 * 
	 * @param options
	 */
	public MatlabProxyFactory(MatlabProxyFactoryOptions options) {
		if (Configuration.isRunningInsideMatlab()) {
			_delegateFactory = new LocalMatlabProxyFactory(options);
		} else {
			_delegateFactory = new RemoteMatlabProxyFactory(options);
		}
	}

	@Override
	public MatlabProxy getProxy() throws MatlabConnectionException {
		return _delegateFactory.getProxy();
	}

	@Override
	public Request requestProxy(RequestCallback callback) throws MatlabConnectionException {
		if (callback == null) {
			throw new NullPointerException("The request callback may not be null");
		}

		return _delegateFactory.requestProxy(callback);
	}

	/**
	 * Provides the requested proxy.
	 * 
	 * @since 4.0.0
	 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
	 */
	public static interface RequestCallback {
		/**
		 * Called when the proxy has been created. Because requests have no timeout, there is no guarantee that this
		 * method will ever be called.
		 * 
		 * @param proxy 
		 */
		public void proxyCreated(MatlabProxy proxy);
	}

	/**
	 * A request for a proxy. Because requests have no timeout, a {@code Request} has no concept of
	 * failure.
	 * <br><br>
	 * Implementations of this class are unconditionally thread-safe.
	 * <br><br>
	 * <b>WARNING:</b> This interface is not intended to be implemented by users of matlabcontrol. Methods may be added
	 * to this interface, and these additions will not be considered breaking binary compatibility.
	 * 
	 * @since 4.0.0
	 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
	 */
	public static interface Request {
		/**
		 * The identifier of the proxy associated with this request. If the proxy is created, then its identifier
		 * accessible via {@link MatlabProxy#getIdentifier()} will return {@code true} when tested for equivalence with
		 * the identifier returned by this method using {@link Identifier#equals(java.lang.Object)}.
		 * 
		 * @return proxy's identifier
		 */
		public Identifier getProxyIdentifer();

		/**
		 * Attempts to cancel the request. If the request has already been completed or cannot successfully be canceled
		 * then {@code false} will be returned, otherwise {@code true} will be returned. If the request has already been
		 * successfully canceled then this method will have no effect and {@code true} will be returned.
		 * 
		 * @return if successfully cancelled
		 */
		public boolean cancel();

		/**
		 * If the request has been successfully cancelled.
		 * 
		 * @return if successfully cancelled
		 */
		public boolean isCancelled();

		/**
		 * Returns {@code true} if the proxy has been created.
		 * 
		 * @return if the proxy has been created
		 */
		public boolean isCompleted();
	}

	/**
	 * A callback interface for receiving the commands which must be
	 * copy-pasted into MATLAB to initiate a connection.
	 */
	public interface CopyPasteCallback {
		/**
		 * The given code should be copy-pasted into MATLAB.
		 */
		void copyPaste(String matlabCmdsToConnect);
	}
}
