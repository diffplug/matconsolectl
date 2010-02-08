package matlab;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This class is initiated by MATLAB. Creates a proxy and sends it to the
 * receiver.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabServer
{
	public MatlabServer(String bindValue)
	{
		try
		{			
			Registry registry = LocateRegistry.getRegistry();
		    MatlabInternalProxyReceiver receiver = (MatlabInternalProxyReceiver) registry.lookup(bindValue);
		    receiver.registerControl(bindValue, new MatlabInternalProxyImpl());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}