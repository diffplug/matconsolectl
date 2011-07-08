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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Represents a receiver for a request to create a proxy. The receiver must be bound to an RMI registry, it will be
 * bound with the RMI identifier {@link #getReceiverID()}. Necessary to have this interface for RMI.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface RequestReceiver extends Remote
{
    /**
     * Receives an incoming wrapper around the JMI functionality inside of MATLAB.
     * <br><br>
     * This method is to be called by {@link MatlabConnector} running inside of MATLAB's JVM.
     * 
     * @param jmiWrapper
     * @param existingSession if the session sending the jmiWrapper was running prior to the request to create the proxy
     * @throws RemoteException 
     */
    public void receiveJMIWrapper(JMIWrapperRemote jmiWrapper, boolean existingSession) throws RemoteException;
    
    /**
     * The identifier of the receiver.
     * 
     * @return
     * @throws RemoteException 
     */
    public String getReceiverID() throws RemoteException;
    
    /**
     * The classpath of the VM the receiver was created in encoded as an RMI codebase.
     * 
     * @return
     * @throws RemoteException 
     */
    public String getClassPathAsRMICodebase() throws RemoteException;
    
    /**
     * The classpath of the VM the receiver was created in encoded as canonical paths.
     * 
     * @return
     * @throws RemoteException 
     */
    public String[] getClassPathAsCanonicalPaths() throws RemoteException;
}