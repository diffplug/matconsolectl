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
 * Allows for Java to communicate with a running MATLAB session. Unexpected behavior may occurs if methods are invoked
 * from multiple threads.
 * <br><br>
 * <strong>Running outside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>an internal MATLAB exception occurs</li>
 * <li>communication between this JVM and the one MATLAB is running in is disrupted (likely due to closing MATLAB)</li>
 * <li>the class of the object to be returned is not {@link java.io.Serializable}</li>
 * <li>the class of the object to be sent or returned is not defined in the JVM receiving the object</li>
 * </ul>
 * <strong>Running inside MATLAB</strong><br>
 * Proxy methods that are relayed to MATLAB may throw exceptions. They will be thrown if:
 * <ul>
 * <li>an internal MATLAB exception occurs</li>
 * <li>the method call is made from the Event Dispatch Thread (EDT) used by AWT and Swing components*</li>
 * </ul>
 * * This is done to prevent MATLAB from becoming non-responsive or hanging indefinitely.
 */
public interface MatlabProxy
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
    
    /**
     * Exits MATLAB.
     * 
     * @throws MatlabInvocationException 
     */
    public void exit() throws MatlabInvocationException;
    
    /**
     * Evaluates a command in MATLAB. The result of this command will not be
     * returned.
     * <br><br>
     * This is equivalent to MATLAB's {@code eval(['command'])}.
     * 
     * @param command the command to be evaluated in MATLAB
     * @throws MatlabInvocationException 
     * 
     * @see #returningEval(String, int)
     */
    public void eval(String command) throws MatlabInvocationException;

    /**
     * Evaluates a command in MATLAB. The result of this command can be returned.
     * <br><br>
     * This is equivalent to MATLAB's {@code eval(['command'])}.
     * <br><br>
     * In order for the result of this command to be returned the number of arguments to be returned must be specified
     * by {@code returnCount}. If the command you are evaluating is a MATLAB function you can determine the amount of
     * arguments it returns by using the {@code nargout} function in the MATLAB Command Window. If it returns
     * {@code -1} that means the function returns a variable number of arguments based on what you pass in. In that
     * case, you will need to manually determine the number of arguments returned. If the number of arguments returned
     * differs from {@code returnCount} then either {@code null} or an empty {@code String} will be returned.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param returnCount the number of arguments that will be returned from evaluating the command
     * 
     * @see #eval(String)
     * 
     * @return result of MATLAB eval
     * @throws MatlabInvocationException 
     */
    public Object returningEval(String command, int returnCount) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}. Arguments to the function may be provided as
     * {@code args}, if you wish to call the function with no arguments pass in {@code null}. The result of this command
     * will not be returned.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @throws MatlabInvocationException 
     * 
     * @see #returningFeval(String, Object[], int)
     * @see #returningFeval(String, Object[])
     */
    public void feval(String functionName, Object[] args) throws MatlabInvocationException;

    /**
     * Calls a MATLAB function with the name {@code functionName}. Arguments to the function may be provided as
     * {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * <br><br>
     * The result of this function can be returned. In order for a function's return data to be returned to MATLAB it is
     * necessary to know how many arguments will be returned. This method will attempt to determine that automatically,
     * but in the case where a function has a variable number of arguments returned it will only return one of them. To
     * have all of them returned use {@link #returningFeval(String, Object[], int)} and specify the number of arguments
     * that will be returned.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     * 
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     */
    public Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}. Arguments to the function may be provided as
     * {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * <br><br>
     * The {@code Object}s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this
     * means that a {@code String} will be converted to a MATLAB char array, not a variable name.
     * <br><br>
     * The result of this function can be returned. In order for the result of this function to be returned the number
     * of arguments to be returned must be specified by {@code returnCount}. You can use the {@code nargout} function in
     * the MATLAB Command Window to determine the number of arguments that will be returned. If {@code nargout} returns
     * {@code -1} that means the function returns a variable number of arguments based on what you pass in. In that
     * case, you will need to manually determine the number of arguments returned. If the number of arguments returned
     * differs from {@code returnCount} then either only some of the items will be returned or {@code null} will be
     * returned.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @param returnCount the number of arguments that will be returned from this function
     * 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     * 
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     */
    public Object returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException;
    
    /**
     * Sets the variable to the given {@code value}.
     * 
     * @param variableName
     * @param value
     * 
     * @throws MatlabInvocationException
     */
    public void setVariable(String variableName, Object value) throws MatlabInvocationException;

    /**
     * Gets the value of the variable named {@code variableName} from MATLAB.
     * 
     * @param variableName
     * 
     * @return value
     * 
     * @throws MatlabInvocationException
     */
    public Object getVariable(String variableName) throws MatlabInvocationException;
    
    /**
     * Allows for enabling a diagnostic mode that will show in MATLAB each time a Java method that calls into MATLAB is
     * invoked.
     * 
     * @param enable
     * @throws MatlabInvocationException 
     */
    public void setDiagnosticMode(boolean enable) throws MatlabInvocationException;
}