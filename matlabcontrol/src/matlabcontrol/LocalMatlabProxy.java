package matlabcontrol;

/*
 * Copyright (c) 2011, Joshua Kaplan
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

/**
 * Allows for calling MATLAB from <b>within</b> MATLAB.
 * <br><br>
 * Methods may be called from any thread; however, calling from the Event
 * Dispatch Thread (EDT) used by AWT and Swing components can be problematic.
 * When a call is made the calling thread is paused. If the call into MATLAB
 * makes use of the EDT then MATLAB will hang indefinitely. The EDT is used
 * extensively by MATLAB when accessing graphical components such as a figure
 * window, uicontrols or plots. 
 * <br><br>
 * The way methods are relayed to MATLAB differs depending on whether or not
 * the methods were invoked on the main MATLAB thread; unexpected behavior may
 * occur if methods are invoked from multiple threads. Any of the methods that
 * are relayed to MATLAB may throw exceptions. Exceptions may be thrown if an
 * internal MATLAB exception occurs.
 * 
 * @author <a href="mailto:jak2@cs.brown.edu">Joshua Kaplan</a>
 */
public final class LocalMatlabProxy implements MatlabProxy
{
    /**
     * The underlying wrapper to JMI.
     */
    private final JMIWrapper _wrapper;
    
    /**
     * Unique identifier for this proxy.
     */
    private final String _id;

    LocalMatlabProxy(JMIWrapper wrapper, String id)
    {
        _wrapper = wrapper;
        _id = id;
    }

    @Override
    public void exit() throws MatlabInvocationException
    {
        _wrapper.exit();
    }
    
    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        _wrapper.eval(command);
    }
    
    @Override
    public Object returningEval(String command, int returnCount) throws MatlabInvocationException
    {
        return _wrapper.returningEval(command, returnCount);
    }
    
    @Override
    public void feval(String functionName, Object[] args) throws MatlabInvocationException
    {
        _wrapper.feval(functionName, args);
    }
    
    @Override
    public Object returningFeval(String functionName, Object[] args) throws MatlabInvocationException
    {
        return _wrapper.returningFeval(functionName, args);
    }

    @Override
    public Object returningFeval(String functionName, Object[] args, int returnCount) throws MatlabInvocationException
    {
        return _wrapper.returningFeval(functionName, args, returnCount);
    }

    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        _wrapper.setVariable(variableName, value);
    }
    
    @Override
    public Object getVariable(String variableName) throws MatlabInvocationException
    {
        return _wrapper.getVariable(variableName);
    }

    @Override
    public void setEchoEval(boolean echo) throws MatlabInvocationException
    {
        _wrapper.setEchoEval(echo);
    }

    @Override
    public String getIdentifier()
    {
        return _id;
    }
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
    
    @Override
    public String toString()
    {
        return "[LocalMatlabProxy identifier:" + _id + "]";
    }
}