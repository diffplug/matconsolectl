package matlab;

/*
 * Copyright (c) 2010, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Passes method calls off to the MatlabControl. This proxy is necessary because
 * fields that MatlabControl uses cannot be marshalled, as is required by RMI.
 * 
 * These methods are documented in MatlabProxy.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabInternalProxyImpl extends UnicastRemoteObject implements MatlabInternalProxy
{
	private static final long serialVersionUID = 1L;
	
	private MatlabController _control;
	
	public MatlabInternalProxyImpl() throws java.rmi.RemoteException
	{
		_control = new MatlabController();
	}
	
	public void exit()
	{
		_control.exit();
	}

	public Object returningFeval(String command, Object[] args) throws RemoteException, InterruptedException
	{
		//Get the number of arguments that will be returned
		Object result = _control.returningFeval("nargout", new String[] { command }, 1);
		int nargout = 0;
		try
		{
			nargout = (int) ((double[]) result)[0];
			
			//If an unlimited number of arguments (represented by -1), choose 1
			if(nargout == -1)
			{
				nargout = 1;
			}
		}
		catch(Exception e) {}
		
		//Send the request
		return _control.returningFeval(command, args, nargout);
	}
	
	public Object returningFeval(String command, Object[] args, int returnCount)  throws RemoteException, InterruptedException
	{
		return _control.returningFeval(command, args, returnCount);
	}
	
	public Object returningEval(String command, int returnCount) throws RemoteException, InterruptedException
	{	
		return _control.returningEval(command, returnCount);
	}

	public void eval(String command) throws RemoteException, InterruptedException
	{
		_control.eval(command);
	}

	public void feval(String command, Object[] args) throws RemoteException, InterruptedException
	{
		_control.feval(command, args);
	}

	public void setEchoEval(boolean echo) throws RemoteException
	{
		_control.setEchoEval(echo);
	}
	
	public void checkConnection() throws RemoteException { }
}