package matlabcontrol.extensions;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 * Wraps around a proxy to conveniently handle casts of the data returned from MATLAB.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class ReturnDataMatlabProxy implements MatlabProxy<ReturnData>
{
    private final MatlabProxy<Object> _delegateProxy;
    
    public ReturnDataMatlabProxy(MatlabProxy<Object> proxy)
    {   
        _delegateProxy = proxy;
    }

    /**
     * Delegates to the proxy.
     * 
     * @return 
     */
    @Override
    public boolean isConnected()
    {
        return _delegateProxy.isConnected();
    }

    /**
     * Delegates to the proxy.
     * 
     * @return 
     */
    @Override
    public String getIdentifier()
    {
        return _delegateProxy.getIdentifier();
    }

    /**
     * Delegates to the proxy.
     * 
     * @throws MatlabInvocationException 
     */
    @Override
    public void exit() throws MatlabInvocationException
    {
        _delegateProxy.exit();
    }

    /**
     * Delegates to the proxy.
     * 
     * @param command
     * @throws MatlabInvocationException 
     */
    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        _delegateProxy.eval(command);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
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
     * Delegates to the proxy.
     * 
     * @param functionName
     * @param args
     * @throws MatlabInvocationException 
     */
    @Override
    public void feval(String functionName, Object[] args) throws MatlabInvocationException
    {
        _delegateProxy.feval(functionName, args);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
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
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
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
     * Delegates to the proxy.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException 
     */
    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        _delegateProxy.setVariable(variableName, value);
    }

    /**
     * Delegates to the proxy, wrapping the result in an instance of {@link ReturnData}.
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
     * Delegates to the proxy.
     * 
     * @param enable
     * @throws MatlabInvocationException 
     */
    @Override
    public void setDiagnosticMode(boolean enable) throws MatlabInvocationException
    {
        this.setDiagnosticMode(enable);
    }
    
    @Override
    public String toString()
    {
        return "[ReturnDataMatlabProxy delegate:" + _delegateProxy + "]";
    }
}