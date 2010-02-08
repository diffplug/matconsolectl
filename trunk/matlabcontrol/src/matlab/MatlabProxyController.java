package matlab;

/**
 * A convenience class that allows for controlling a single session of MATLAB
 * without having to create a MatlabProxyFactory and then create a MatlabProxy.
 * 
 * @see MatlabProxyFactory
 * @see MatlabProxy
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public class MatlabProxyController
{
	private static MatlabProxyFactory _factory;
	private static MatlabProxy _proxy;
	
	private MatlabProxyController() {}
	
	/**
	 * Create a connection to MATLAB. If a connection already exists this
	 * method will not do anything. This must be called before any methods
	 * that control MATLAB are called, or those methods will throw runtime
	 * exceptions.
	 * 
	 * @throws MatlabConnectionException
	 */
	public static void createConnection() throws MatlabConnectionException
	{
		if(!MatlabProxyController.isConnected())
		{
			_factory = new MatlabProxyFactory();
			
			_factory.addConnectionListener(new MatlabConnectionListener()
			{
				public void connectionEstablished(MatlabProxy proxy)
				{
					_proxy = proxy;
				}
	
				public void connectionLost(MatlabProxy proxy)
				{
					_proxy = null;
				}
			});
			
			_factory.getProxy();
		}
	}
	
	/**
	 * Set the location of the MATLAB program. If this property is not set an
	 * appropriate default for your operating system will be used. If that
	 * fails, then use this method to give the correct location.
	 * 
	 * @param matlabLocation
	 */
	public static void setMatlabLocation(String matlabLocation)
	{
		_factory.setMatlabLocation(matlabLocation);
	}
	
	/**
	 * Sets the maximum amount of time to wait in attempting to setup a
	 * connection to MATLAB in milliseconds. The default value is 60
	 * seconds.
	 * 
	 * @param ms
	 */
	public static void setTimeout(int ms)
	{
		_factory.setTimeout(ms);
	}
	
	/**
	 * Returns whether or not this controller is connected to MATLAB.
	 *
	 * @return if connected to MATLAB
	 */
	public static boolean isConnected()
	{
		return (_proxy != null && _proxy.isConnected());
	}
	
    /**
     * Evaluate a string, MATLAB script, or MATLAB function
     * 
     * @param command
     */
	public static void eval(String command)
	{
		if(MatlabProxyController.isConnected())
		{
			_proxy.eval(command);
		}
		else
		{
			throw new MatlabCommandException("There is no connection to MATLAB");
		}
	}
	
    /**
     * Evaluate a MATLAB function that requires arguments.  Each element of
     * the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     */
	public static void feval(String command, Object[] args)
	{
		if(MatlabProxyController.isConnected())
		{
			_proxy.feval(command, args);
		}
		else
		{
			throw new MatlabCommandException("There is no connection to MATLAB");
		}
	}
	
    /**
     * Evaluate a MATLAB function that requires arguments and provide return arg.
     * Each element of the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     * 
     * @return result of function, may be null
     */
	public static Object blockingFeval(String command, Object[] args)
	{
		if(MatlabProxyController.isConnected())
		{
			return _proxy.blockingFeval(command, args);
		}
		else
		{
			throw new MatlabCommandException("There is no connection to MATLAB");
		}
	}
	
    /**
     * Echoing the eval statement is useful if you want to see in
     * MATLAB each time that a java function tries to execute a MATLAB
     * command.
     * 
     * @param echo
     */
	public static void setEchoEval(boolean echo)
	{		
		if(MatlabProxyController.isConnected())
		{
			_proxy.setEchoEval(echo);
		}
		else
		{
			throw new MatlabCommandException("There is no connection to MATLAB");
		}
	}
}