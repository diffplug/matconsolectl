package demo.gui;

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
 * Represents a method in the matlabcontrol API along with information to adjust the GUI.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public enum ProxyMethodDescriptor
{
    
    EVAL("void eval(String command)", Documentation.EVAL, "command", "args (disabled)", false, 0),
    RETURNING_EVAL("Object returningEval(String command, int returnCount)", Documentation.RETURNING_EVAL, "comamnd", "args (disabled)", true, 0),
    FEVAL("void feval(String functionName, Object[] args)", Documentation.FEVAL, "functionName", "args", false, ArrayPanel.NUM_ENTRIES),
    RETURNING_AUTO_FEVAL("Object returningFeval(String functionName, Object[] args)", Documentation.AUTO_RETURNING_FEVAL, "functionName", "args", false, ArrayPanel.NUM_ENTRIES),
    RETURNING_FEVAL("Object returningFeval(String functionName, Object[] args, int returnCount)", Documentation.RETURNING_FEVAL, "functionName", "args", true, ArrayPanel.NUM_ENTRIES),
    SET_VARIABLE("void setVariable(String variableName, Object value)", Documentation.SET_VARIABLE, "variableName", "value", false, 1),
    GET_VARIABLE("Object getVariable(String variableName)", Documentation.GET_VARIABLE, "variableName", "args (disabled)", false, 0);

    
    private ProxyMethodDescriptor(String signature, String message, String stringInputName,
            String argsInputName, boolean returnCountEnabled, int argsInputNumberEnabled)
    {
        this.signature = signature;
        this.message = message;
        this.stringInputName = stringInputName;
        this.argsInputName = argsInputName;
        this.returnCountEnabled = returnCountEnabled;
        this.argsInputNumberEnabled = argsInputNumberEnabled;
    }
    
    /**
     * Method signature
     */
    public final String signature;
    
    /**
     * Description of method.
     */
    public final String message;
    
    /**
     * Text that surrounds the border of the text input field.
     */
    public final String stringInputName;
    
    /**
     * Text that surrounds the border of the arguments input field.
     */
    public final String argsInputName;
    
    /**
     * Whether the return count field is enabled.
     */
    public final boolean returnCountEnabled;
    
    /**
     * Number of entries enabled in the argument input field.
     */
    public final int argsInputNumberEnabled;
    
    @Override
    public String toString()
    {
        return signature;
    }
    
    private static class Documentation
    {
        private static final String
        EVAL =
        "<html>Evaluates a command in MATLAB. This is equivalent to MATLAB's <tt>eval('command')</tt>.</html>",

        RETURNING_EVAL =
        "<html>Evaluates a command in MATLAB, returning the result. This is equivalent to MATLAB's <tt>eval('command')</tt>." +
        "<br><br>" +
        "In order for the result of this command to be returned the number of arguments to be returned must be specified " +
        "by <tt>returnCount</tt>. If the command you are evaluating is a MATLAB function you can determine the amount of " +
        "arguments it returns by using the <tt>nargout</tt> function in the MATLAB Command Window. If it returns " +
        "<tt>-1</tt> that means the function returns a variable number of arguments. In that case, you will need to " +
        "manually determine the appropriate number of arguments returned for your use. If the number of arguments MATLAB " +
        "attempts to return differs <tt>returnCount</tt> an undefined behavior will occur. Among the behaviors possible " +
        "is an exception being thrown, <tt>null</tt> being returned, or an empty <tt>String</tt> being returned.</html>",

        FEVAL =
        "<html>Calls a MATLAB function with the name <tt>functionName</tt>. Arguments to the function may be provided as " +
        "<tt>args</tt>, if you wish to call the function with no arguments pass in <tt>null</tt>." +
        "<br><br>" +
        "The <tt>Object</tt>s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this " +
        "means that a <tt>String</tt> will be converted to a MATLAB <tt>char</tt> array, not a variable name.</html>",

        AUTO_RETURNING_FEVAL =
        "<html>Calls a MATLAB function with the name <tt>functionName</tt>, returning the result. Arguments to the function may " +
        "be provided as <tt>args</tt>, if you wish to call the function with no arguments pass in <tt>null</tt>." +
        "<br><br>" +
        "The <tt>Object</tt>s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this " +
        "means that a <tt>String</tt> will be converted to a MATLAB <tt>char</tt> array, not a variable name." +
        "<br><br>" +
        "This method may only be used for if <tt>functionName</tt> returns a fixed number of arguments. If " +
        "<tt>functionName</tt> returns a variable number of arguments an exception will be thrown. To successfully call " +
        "<tt>functionName</tt> use <tt>returningFeval(String, Object[], int)</tt> and specify the " +
        "number of returned arguments.</html>",

        RETURNING_FEVAL =
        "<html>Calls a MATLAB function with the name <tt>functionName</tt>, returning the result. Arguments to the function may " +
        "be provided as <tt>args</tt>, if you wish to call the function with no arguments pass in <tt>null</tt>." +
        "<br><br>" +
        "The <tt>Object</tt>s in the array will be converted into MATLAB equivalents as appropriate. Importantly, this " +
        "means that a <tt>String</tt> will be converted to a MATLAB <tt>char</tt> array, not a variable name. " +
        "<br><br>" +
        "In order for the result of this function to be returned the number of arguments to be returned must be specified " +
        "by <tt>returnCount</tt>. You can use the <tt>nargout</tt> function in the MATLAB Command Window to determine the " +
        "number of arguments that will be returned. If <tt>nargout</tt> returns <tt>-1</tt> that means the function returns " + 
        "a variable number of arguments. In that case, you will need to manually determine the appropriate number of " +
        "arguments returned for your use. If the number of arguments MATLAB attempts to return differs <tt>returnCount</tt> " +
        "an undefined behavior will occur. Among the behaviors possible is an exception being thrown, <tt>null</tt> being " +
        "returned, or an empty <tt>String</tt> being returned.</html>",

        SET_VARIABLE = 
        "<html>Sets <tt>variableName</tt> to <tt>value</tt> in MATLAB, creating the variable if it does not yet exist.</html>",

        GET_VARIABLE = 
        "<html>Gets the value of <tt>variableName</tt> in MATLAB.</html>";
    }
}