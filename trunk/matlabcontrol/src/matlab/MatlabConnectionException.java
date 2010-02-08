package matlab;

/**
 * Exception that represents a failure to launch and connect to MATLAB.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public class MatlabConnectionException extends Exception
{
	private static final long serialVersionUID = 1L;

	MatlabConnectionException(String msg)
	{
		super(msg);
	}
	
	MatlabConnectionException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}