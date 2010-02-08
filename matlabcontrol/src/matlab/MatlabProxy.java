package matlab;

import java.rmi.RemoteException;

/**
 * This class allows you to call methods that will be relayed to MATLAB. Any of
 * the methods that are relayed to MATLAB may throw runtime exceptions. They
 * will be thrown if something occurs to disrupt the communication between this
 * JVM and the one MATLAB is running in. For instance, closing MATLAB will
 * terminate its JVM and then all method calls on this proxy will throw
 * runtime exceptions.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public class MatlabProxy
{
	private MatlabInternalProxy _internalProxy;
	
	MatlabProxy(MatlabInternalProxy internalProxy)
	{
		_internalProxy = internalProxy;
	}
	
    /**
     * Evaluate a MATLAB function that requires arguments and provide return arg.
     * Each element of the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     */
	public Object blockingFeval(String command, Object[] args)
	{
		try
		{
			return _internalProxy.blockingFeval(command, args);
		}
		catch (RemoteException e)
		{
			if(this.isConnected())
			{
				throw new MatlabCommandException("Method could not be invoked for an unknown reason",e);
			}
			else
			{
				throw new MatlabCommandException("This proxy is no longer connected to MATLAB", e);
			}
		}
		catch (InterruptedException e)
		{
			throw new MatlabCommandException("Method could not be invoked because the thread was interrupted before MATLAB returned a value",e);
		}
	}

    /**
     * Evaluate a string, MATLAB script, or MATLAB function
     * 
     * @param command
     */
	public void eval(String command)
	{
		try
		{
			_internalProxy.eval(command);
		}
		catch (RemoteException e)
		{
			if(this.isConnected())
			{
				throw new MatlabCommandException("Method could not be invoked for an unknown reason",e);
			}
			else
			{
				throw new MatlabCommandException("This proxy is no longer connected to MATLAB", e);
			}
		}
	}

    /**
     * Evaluate a MATLAB function that requires arguments.  Each element of
     * the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     */
	public void feval(String command, Object[] args)
	{
		try
		{
			_internalProxy.feval(command, args);
		}
		catch (RemoteException e)
		{
			if(this.isConnected())
			{
				throw new MatlabCommandException("Method could not be invoked for an unknown reason",e);
			}
			else
			{
				throw new MatlabCommandException("This proxy is no longer connected to MATLAB", e);
			}
		}
	}

    /**
     * Echoing the eval statement is useful if you want to see in
     * MATLAB each time that a java function tries to execute a MATLAB
     * command.
     * 
     * @param echo
     */
	public void setEchoEval(boolean echo)
	{
		try
		{
			_internalProxy.setEchoEval(echo);
		}
		catch (RemoteException e)
		{
			if(this.isConnected())
			{
				throw new MatlabCommandException("Method could not be invoked for an unknown reason",e);
			}
			else
			{
				throw new MatlabCommandException("This proxy is no longer connected to MATLAB", e);
			}
		}
	}
	
	/**
	 * Whether this proxy is connected to MATLAB. The most likely reason this
	 * method would return false is if MATLAB has been closed.
	 * 
	 * @return if connected to MATLAB
	 */
	public boolean isConnected()
	{
		try
		{
			_internalProxy.checkConnection();	
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
}