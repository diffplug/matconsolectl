package matlabcontrol;

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

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This class is called on from MATLAB. Creates a proxy and sends it to the
 * receiver.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
class MatlabConnector
{
	/**
	 * Private constructor so this class cannot be constructed.
	 */
	private MatlabConnector() { } 
	
	/**
	 * The underlying wrapper that interacts with the Java MATLAB Interface.
	 */
	private static JMIWrapper _wrapper;
	
	/**
	 * Returns the underlying wrapper.
	 * 
	 * @return
	 */
	static JMIWrapper getWrapper()
	{
		return _wrapper;
	}
	
	/**
	 * Called from MATLAB to create a controller, wrap it in a proxy, and then send it over RMI
	 * to the Java program running in a separate JVM.
	 * 
	 * @param bindValue
	 * 
	 * @throws MatlabConnectionException
	 */
	public static void connectFromMatlab(String bindValue) throws MatlabConnectionException
	{
		try
		{	
			//Get the receiver from the registry
			Registry registry = LocateRegistry.getRegistry();
		    MatlabInternalProxyReceiver receiver = (MatlabInternalProxyReceiver) registry.lookup(bindValue);
		    
		    //Create the wrapper, then pass the internal proxy over RMI to the Java application in its own JVM
		    _wrapper = new JMIWrapper();
		    receiver.registerControl(bindValue, new MatlabInternalProxyImpl(_wrapper));
		}
		catch (Exception e)
		{
			//If connection could not be established, let the wrapper be garbage collected
			_wrapper = null;
			
			//Throw exception that indicates connection could be established
			throw new MatlabConnectionException("Connection to Java application could not be established", e);
		}
	}
}