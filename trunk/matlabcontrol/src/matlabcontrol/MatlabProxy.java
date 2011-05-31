package matlabcontrol;

/**
 *
 * 
 * 
 */
public interface MatlabProxy
{
    public boolean isConnected();
    
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
     * @param args the arguments to the function, <code>null</code> if none
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
     * @param echo
     * @throws MatlabInvocationException 
     */
    public void setEchoEval(boolean echo) throws MatlabInvocationException;
}