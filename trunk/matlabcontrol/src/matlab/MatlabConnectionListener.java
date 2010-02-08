package matlab;

/**
 * Implement this interface to be informed when a connection has been
 * established or lost.
 * 
 * @see MatlabProxyFactory
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public interface MatlabConnectionListener
{
	/**
	 * Called when the connection to the proxy has been established.
	 * 
	 * @param proxy the proxy created when the connection was established
	 */
	public void connectionEstablished(MatlabProxy proxy);
	
	/**
	 * Called when the connection to the proxy has been lost.
	 * To get a new connection, use an instance of {@link MatlabProxyFactory}.
	 * 
	 * @param proxy the proxy that is no longer connected
	 */
	public void connectionLost(MatlabProxy proxy);
}