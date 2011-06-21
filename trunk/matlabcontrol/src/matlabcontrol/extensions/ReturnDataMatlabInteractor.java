package matlabcontrol.extensions;

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

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabInteractor;

/**
 * Wraps around an interactor to conveniently handle casts of the data returned from MATLAB.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class ReturnDataMatlabInteractor implements MatlabInteractor<ReturnData>
{
    private final MatlabInteractor<?> _delegateInteractor;
    
    public ReturnDataMatlabInteractor(MatlabInteractor<?> interactor)
    {   
        _delegateInteractor = interactor;
    }

    /**
     * Delegates to the interactor.
     * 
     * @throws MatlabInvocationException 
     */
    @Override
    public void exit() throws MatlabInvocationException
    {
        _delegateInteractor.exit();
    }

    /**
     * Delegates to the interactor.
     * 
     * @param command
     * @throws MatlabInvocationException 
     */
    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        _delegateInteractor.eval(command);
    }

    /**
     * Delegates to the interactor, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param command
     * @param returnCount
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public ReturnData returningEval(String command, int returnCount) throws MatlabInvocationException
    {
        return new ReturnData(this.returningEval(command, returnCount));
    }

    /**
     * Delegates to the interactor.
     * 
     * @param functionName
     * @param args
     * @throws MatlabInvocationException 
     */
    @Override
    public void feval(String functionName, Object[] args) throws MatlabInvocationException
    {
        _delegateInteractor.feval(functionName, args);
    }

    /**
     * Delegates to the interactor, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param functionName
     * @param args
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public ReturnData returningFeval(String functionName, Object[] args) throws MatlabInvocationException
    {
        return new ReturnData(this.returningFeval(functionName, args));
    }

    /**
     * Delegates to the interactor, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param functionName
     * @param args
     * @param returnCount
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public ReturnData returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException
    {
        return new ReturnData(this.returningFeval(functionName, args, returnCount));
    }

    /**
     * Delegates to the interactor.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException 
     */
    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        _delegateInteractor.setVariable(variableName, value);
    }

    /**
     * Delegates to the interactor, wrapping the result in an instance of {@link ReturnData}.
     * 
     * @param variableName
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public ReturnData getVariable(String variableName) throws MatlabInvocationException
    {
        return new ReturnData(this.getVariable(variableName));
    }
    
    /**
     * Delegates to the interactor.
     * 
     * @param obj
     * @param storePermanently
     * @return
     * @throws MatlabInvocationException 
     */
    @Override
    public String storeObject(Object obj, boolean storePermanently) throws MatlabInvocationException
    {
        return _delegateInteractor.storeObject(obj, storePermanently);
    }
    
    @Override
    public String toString()
    {
        return "[ReturnDataMatlabInteractor delegate=" + _delegateInteractor + "]";
    }
}