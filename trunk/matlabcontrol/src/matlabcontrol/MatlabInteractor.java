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
 * Interacts with a session of MATLAB. This interface exists to facilitate creating wrappers around a
 * {@link MatlabProxy} that in turn can be provided to another wrapper. The {@link matlabcontrol.extensions} package
 * contains several classes which do exactly that. Example usage:
 * <pre>
 * {@code 
 * MatlabProxy proxy = factory.getProxy();
 * ReturnDataMatlabInteractor returnInteractor = new ReturnDataMatlabInteractor(proxy);
 * MatlabCallbackInteractor<ReturnData> callbackInteractor = new MatlabCallbackInteractor<ReturnData>(returnInteractor);
 * }
 * </pre>
 * All methods defined in this interface may throw a {@link MatlabInvocationException} if the {@code eval} or
 * {@code feval} statement cannot be successfully completed.
 * 
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * @param T the type of data returned by methods which return values from MATLAB
 */
public interface MatlabInteractor<T>
{    
    /**
     * Evaluates a command in MATLAB.
     * 
     * @param command the command to be evaluated in MATLAB
     * @throws MatlabInvocationException 
     * 
     * @see #returningEval(String, int)
     */
    public void eval(String command);

    /**
     * Evaluates a command in MATLAB, returning the result.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param returnCount the number of arguments that will be returned from evaluating the command
     * @return result of MATLAB {@code eval}
     * @throws MatlabInvocationException 
     * 
     * @see #eval(String)
     */
    public T returningEval(String command, int returnCount);
    
    /**
     * Calls a MATLAB function with the name {@code functionName}. Arguments to the function may be provided as
     * {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @throws MatlabInvocationException 
     * 
     * @see #returningFeval(String, Object[], int)
     * @see #returningFeval(String, Object[])
     */
    public void feval(String functionName, Object[] args);

    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, if you wish to call the function with no arguments pass in {@code null}.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     * 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     */
    public T returningFeval(String functionName, Object[] args);
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, if you wish to call the function with no arguments pass in {@code null}. Specifies
     * the number of arguments returned as {@code returnCount}.
     * 
     * @param functionName name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @param returnCount the number of arguments that will be returned from this function
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     * 
     * @see #feval(String, Object[])
     * @see #returningFeval(String, Object[])
     */
    public T returningFeval(String functionName, Object[] args, int returnCount);
    
    /**
     * Sets {@code variableName} to {@code value} in MATLAB, creating the variable if it does not yet exist.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException
     */
    public void setVariable(String variableName, Object value);
    
    /**
     * Gets the value of {@code variableName} in MATLAB.
     * 
     * @param variableName
     * @return value
     * @throws MatlabInvocationException
     */
    public T getVariable(String variableName);
    
    /**
     * Runs the {@code callable} in MATLAB, returning the result of the {@code callable}.
     * 
     * @param <U> the type of data returned by the callable
     * @param callable
     * @return result of the callable
     * @throws MatlabInvocationException 
     */
    public <U> U invokeAndWait(MatlabCallable<U> callable);
    
    /**
     * Computation performed in MATLAB.
     * 
     * @param <U> type of the data returned by the callable
     */
    public static interface MatlabCallable<U>
    {
        /**
         * Performs the computation in MATLAB.
         * 
         * @param interactor
         * @return
         * @throws MatlabInvocationException 
         */
        public U call(MatlabInteractor<Object> interactor);
    }
}