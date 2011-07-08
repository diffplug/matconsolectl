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
 * Interacts with a session of MATLAB.
 * 
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
interface MatlabInteractor
{
    /**
     * Evaluates a command in MATLAB. This is equivalent to MATLAB's {@code eval('command')}.
     * 
     * @param command the command to be evaluated in MATLAB
     * @throws MatlabInvocationException 
     */
    public void eval(String command) throws MatlabInvocationException;

    /**
     * Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's {@code eval('command')}.
     * <br><br>
     * In order for the result of this command to be returned the number of arguments to be returned must be specified
     * by {@code nargout}. This is equivalent in MATLAB to the number of variables placed on the left hand side of an
     * expression. For example, in MATLAB the {@code inmem} function may be used with either 1, 2, or 3 return values
     * each resulting in a different behavior:
     * <pre>
     * {@code
     * M = inmem;
     * [M, X] = inmem;
     * [M, X, J] = inmem;
     * }
     * </pre>
     * The returned {@code Object} array will be of length {@code nargout} with each return argument placed into the
     * corresponding array position.
     * <br><br>
     * If the command cannot return the number of arguments specified by {@code nargout} then an exception will be
     * thrown.
     * 
     * @param command the command to be evaluated in MATLAB
     * @param nargout the number of arguments that will be returned from evaluating {@code command}
     * @return result of MATLAB command, the length of the array will be {@code nargout}
     * @throws MatlabInvocationException
     */
    public Object[] returningEval(String command, int nargout) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, but are not required if the function needs no arguments.
     * <br><br>
     * The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means that a
     * {@code String} will be converted to a MATLAB {@code char} array, not a variable name.
     * 
     * @param functionName the name of the MATLAB function to call
     * @param args the arguments to the function
     * @throws MatlabInvocationException 
     */
    public void feval(String functionName, Object... args) throws MatlabInvocationException;
    
    /**
     * Calls a MATLAB function with the name {@code functionName}, returning the result. Arguments to the function may
     * be provided as {@code args}, but are not required if the function needs no arguments.
     * <br><br>
     * The function arguments will be converted into MATLAB equivalents as appropriate. Importantly, this means that a
     * {@code String} will be converted to a MATLAB {@code char} array, not a variable name.
     * <br><br>
     * In order for the result of this function to be returned the number of arguments to be returned must be specified
     * by {@code nargout}. This is equivalent in MATLAB to the number of variables placed on the left hand side of an
     * expression. For example, in MATLAB the {@code inmem} function may be used with either 1, 2, or 3 return values
     * each resulting in a different behavior:
     * <pre>
     * {@code
     * M = inmem;
     * [M, X] = inmem;
     * [M, X, J] = inmem;
     * }
     * </pre>
     * The returned {@code Object} array will be of length {@code nargout} with each return argument placed into the
     * corresponding array position.
     * <br><br>
     * If the function is not capable of returning the number of arguments specified by {@code nargout} then an
     * exception will be thrown.
     * 
     * @param functionName the name of the MATLAB function to call
     * @param nargout the number of arguments that will be returned by {@code functionName}
     * @param args the arguments to the function
     * @return result of MATLAB function, the length of the array will be {@code nargout}
     * @throws MatlabInvocationException 
     */
    public Object[] returningFeval(String functionName, int nargout, Object... args) throws MatlabInvocationException;
    
    /**
     * Sets {@code variableName} to {@code value} in MATLAB, creating the variable if it does not yet exist.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException
     */
    public void setVariable(String variableName, Object value) throws MatlabInvocationException;
    
    /**
     * Gets the value of {@code variableName} in MATLAB.
     * 
     * @param variableName
     * @return value
     * @throws MatlabInvocationException
     */
    public Object getVariable(String variableName) throws MatlabInvocationException;
}