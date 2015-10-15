/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.util.concurrent.atomic.AtomicInteger;

import matlabcontrol.MatlabProxy.Identifier;
import matlabcontrol.MatlabProxyFactory.Request;
import matlabcontrol.MatlabProxyFactory.RequestCallback;

/**
 * Creates local instances of {@link MatlabProxy}.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalMatlabProxyFactory implements ProxyFactory {
	public LocalMatlabProxyFactory(MatlabProxyFactoryOptions options) {}

	@Override
	public LocalMatlabProxy getProxy() throws MatlabConnectionException {
		JMIValidator.validateJMIMethods();

		return new LocalMatlabProxy(new LocalIdentifier());
	}

	@Override
	public Request requestProxy(RequestCallback requestCallback) throws MatlabConnectionException {
		LocalMatlabProxy proxy = getProxy();
		requestCallback.proxyCreated(proxy);

		return new LocalRequest(proxy.getIdentifier());
	}

	private static final class LocalIdentifier implements Identifier {
		private static final AtomicInteger PROXY_CREATION_COUNTER = new AtomicInteger();

		private final int _id = PROXY_CREATION_COUNTER.getAndIncrement();

		@Override
		public boolean equals(Object other) {
			boolean equals;

			if (other instanceof LocalIdentifier) {
				equals = (((LocalIdentifier) other)._id == _id);
			} else {
				equals = false;
			}

			return equals;
		}

		@Override
		public int hashCode() {
			return _id;
		}

		@Override
		public String toString() {
			return "PROXY_LOCAL_" + _id;
		}
	}

	private static final class LocalRequest implements Request {
		private final Identifier _proxyID;

		private LocalRequest(Identifier proxyID) {
			_proxyID = proxyID;
		}

		@Override
		public Identifier getProxyIdentifer() {
			return _proxyID;
		}

		@Override
		public boolean cancel() {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isCompleted() {
			return true;
		}
	}
}
