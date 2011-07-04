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
public interface MatlabInteractor
{    
    /**
     * Evaluates a command in MATLAB. This is equivalent to MATLAB's {@code eval('command')}.
     * 
     * @param command the command to be evaluated in MATLAB
     * @throws MatlabInvocationException 
     */
    public abstract void eval(String command) throws MatlabInvocationException;

    /**
     * Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's {@code eval('command')}.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param nargout the number of arguments that will be returned from evaluating {@code command}
     * @return result of MATLAB command
     * @throws MatlabInvocationException
     */
    public abstract Object[] returningEval(String command, int nargout) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, but are not required if the function needs no arguments.
     * 
     * @param functionName the name of the MATLAB function to call
     * @param args the arguments to the function, {@code null} if none
     * @throws MatlabInvocationException 
     */
    public abstract void feval(String functionName, Object... args) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, but are not required if the function needs no arguments.
     * 
     * @param functionName the name of the MATLAB function to call
     * @param nargout the number of arguments that will be returned by {@code functionName}
     * @param args the arguments to the function
     * @return result of MATLAB function
     * @throws MatlabInvocationException 
     */
    public abstract Object[] returningFeval(String functionName, int nargout, Object... args)
            throws MatlabInvocationException;
    
    /**
     * Sets {@code variableName} to {@code value} in MATLAB, creating the variable if it does not yet exist.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException
     * 
     * @see #getVariable(String) 
     */
    public void setVariable(String variableName, Object value) throws MatlabInvocationException;
    
    /**
     * Gets the value of {@code variableName} in MATLAB.
     * 
     * @param variableName
     * @return value
     * @throws MatlabInvocationException
     * @see #setVariable(String, Object)
     */
    public Object getVariable(String variableName) throws MatlabInvocationException;
    
    /**
     * Runs the {@code callable} in MATLAB, returning the result of the {@code callable}.
     * 
     * @param <U> the type of data returned by the callable
     * @param callable
     * @return result of the callable
     * @throws MatlabInvocationException 
     */
    public <U> U invokeAndWait(MatlabCallable<U> callable) throws MatlabInvocationException;
    
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
         * @return result of the computation
         * @throws MatlabInvocationException 
         */
        public U call(MatlabInteractor interactor) throws MatlabInvocationException;
    }
}