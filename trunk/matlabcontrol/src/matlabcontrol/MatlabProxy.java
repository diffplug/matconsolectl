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

/**
 * Allows for Java to communicate with a running MATLAB session.
 * <br><br>
 * Implementations of this interface are thread-safe. While methods may be called concurrently, they will be completed
 * sequentially on MATLAB's main thread. More than one proxy may be interacting with MATLAB (for instance one proxy
 * might be running inside MATLAB and another might be running outside MATLAB) and their interactions will not occur
 * simultaneously. Regardless of the number of proxies, interaction with MATLAB occurs in a single threaded manner.
 * <br><br>
 * <strong>Running outside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>an internal MATLAB exception occurs*</li>
 * <li>communication between this JVM and the one MATLAB is running in is disrupted (likely due to closing MATLAB)</li>
 * <li>the class of the object to be returned is not {@code java.io.Serializable}</li>
 * <li>the class of the object to be sent or returned is not defined in the JVM receiving the object</li>
 * </ul>
 * <strong>Running inside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>an internal MATLAB exception occurs*</li>
 * <li>the method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components
 *     (This is done to prevent MATLAB from becoming non-responsive or hanging indefinitely.)</li>
 * </ul>
 * *Internal MATLAB exceptions occur primarily for two reasons:
 * <ul>
 * <li>There is an error in the MATLAB code executing. An example of this includes attempting to call a function that
 *     does not exist (frequently due to a typo).</li>
 * <li>The Java MATLAB Interface (JMI) becoming out of sync with MATLAB. An example of this is an exception being thrown
 *     when attempting to retrieve or make use of a variable that has already been defined.</li>
 * </ul>
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public interface MatlabProxy extends MatlabInteractor<Object>
{
    /**
     * Whether this proxy is connected to MATLAB.
     * <br><br>
     * The most likely reasons for this method to return {@code false} is if MATLAB has been closed or the factory that
     * created this proxy has been shutdown.
     * 
     * @return 
     */
    public boolean isConnected();
    
    /**
     * Returns the unique identifier for this proxy.
     * <br><br>
     * If this proxy was created by a call to {@link MatlabProxyFactory#requestProxy()}, then the {@code String} this
     * method returns matches that returned when calling to create this proxy.
     * 
     * @return identifier
     */
    public String getIdentifier();
}