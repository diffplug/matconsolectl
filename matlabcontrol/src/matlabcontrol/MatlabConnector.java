package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This class is used only from inside of the MATLAB JVM. It is responsible for creating proxies and sending them to
 * the receiver over RMI.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabConnector
{
    private static JMIWrapper _wrapper = null;
    
    /**
     * Private constructor so this class cannot be constructed.
     */
    private MatlabConnector() { }
    
    static JMIWrapper getJMIWrapper()
    {
        if(_wrapper == null)
        {
            _wrapper = new JMIWrapper();
        }
        
        return _wrapper;
    }
    
    /**
     * Called from MATLAB to create a proxy. Creates the proxy and then sends it over RMI to the Java program running in
     * a separate JVM.
     * 
     * @param receiverID the key that binds the receiver in the registry
     * @param proxyID the unique identifier of the proxy being created
     * 
     * @throws MatlabConnectionException
     */
    public static void connectFromMatlab(String receiverID, String proxyID) throws MatlabConnectionException
    {   
        //Register this session of MATLAB
        MatlabBroadcaster.broadcast();
        
        //Attempt to connect to external Java program and transmit proxy
        try
        {                
            //Get registry
            Registry registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
            
            //Get the receiver from the registry
            JMIWrapperRemoteReceiver receiver = (JMIWrapperRemoteReceiver) registry.lookup(receiverID);
            
            //Register the receiver with broadcaster
            MatlabBroadcaster.addReceiver(receiver); 
            
            //Create the remote JMI wrapper and then pass it over RMI to the Java application in its own JVM
            receiver.registerControl(proxyID, new JMIWrapperRemoteImpl(getJMIWrapper()));
        }
        //If for any reason the attempt fails, throw exception that indicates connection could not be established
        catch(Exception e)
        {
            throw new MatlabConnectionException("Connection to Java application could not be established", e);
        }
    }
}