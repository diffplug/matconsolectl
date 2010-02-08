package matlab;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Methods that can be called to control MATLAB. All of these methods
 * throw RemoteException. RemoteException will be thrown if something
 * occurs to disrupt the communication between this JVM and the one
 * MATLAB is running in. For instance, closing MATLAB will terminate
 * its JVM and then all method calls on this proxy will throw exceptions.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
interface MatlabInternalProxy extends Remote
{
    /**
     * Evaluate a string, MATLAB script, or MATLAB function
     * 
     * @param command
     * 
     * @throws RemoteException
     */
	public void eval(String command) throws RemoteException;
	
    /**
     * Evaluate a MATLAB function that requires arguments.  Each element of
     * the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     * 
     * @throws RemoteException
     */
	public void feval(String command, Object[] args) throws RemoteException;
	
    /**
     * Evaluate a MATLAB function that requires arguments and provide return arg.
     * Each element of the "args" array is an argument to the function "command"
     * 
     * @param command
     * @param args
     * 
     * @throws RemoteException
     * @throws InterruptedException
     */
	public Object blockingFeval(String command, Object[] args) throws RemoteException, InterruptedException;
	
    /**
     * Echoing the eval statement is useful if you want to see in
     * MATLAB each time that a java function tries to execute a MATLAB
     * command.
     * 
     * @param echo
     * 
     * @throws RemoteException
     */
	public void setEchoEval(boolean echo) throws RemoteException;
	
	/**
	 * This method does nothing. It is used internally to check if a connection
	 * is still active.
	 * 
	 * @throws RemoteException
	 */
	public void checkConnection() throws RemoteException;
}