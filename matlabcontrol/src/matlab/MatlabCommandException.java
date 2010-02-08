package matlab;

/**
 * An exception that occurs when attempting to invoke a method on the MATLAB
 * session but the invocation fails. This will most likely occur if the
 * MATLAB session being controlled is no longer open.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public class MatlabCommandException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public MatlabCommandException(String msg)
	{
		super(msg);
	}
	
	public MatlabCommandException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}