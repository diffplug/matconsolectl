/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import matlabcontrol.MatlabProxyFactory.Request;
import matlabcontrol.MatlabProxyFactory.RequestCallback;

/**
 * A factory which creates instances of {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface ProxyFactory {
	/**
	 * Returns a {@link MatlabProxy}. If a connection cannot be established before the timeout then this method will end
	 * execution and an exception will be thrown. A timeout can be specified with the options provided to this factory.
	 * If no timeout was specified, then a default of 180 seconds will be used.
	 * <br><br>
	 * While this method blocks the calling thread until a proxy is created (or the timeout is reached), any number of
	 * threads may call {@code getProxy()} simultaneously.
	 * 
	 * @throws MatlabConnectionException
	 * @return proxy
	 */
	public MatlabProxy getProxy() throws MatlabConnectionException;

	/**
	 * Requests a {@link MatlabProxy}. When the proxy has been created it will be provided to the {@code callback}. The
	 * proxy may be provided to the callback before this method returns. There is no timeout. The returned
	 * {@link Request} instance provides information about the request and can be used to cancel the request.
	 * <br><br>
	 * This method is non-blocking. Any number of requests may be made simultaneously from the same thread or different
	 * threads.
	 * 
	 * @throws MatlabConnectionException
	 * @return request
	 */
	public Request requestProxy(RequestCallback callback) throws MatlabConnectionException;
}
