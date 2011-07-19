package matlabcontrol.link;

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

import java.util.HashSet;
import java.util.Set;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.link.MatlabType.MatlabTypeSerializationProvider;
import matlabcontrol.link.MatlabType.MatlabTypeSerializedSetter;

/**
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@MatlabTypeSerializationProvider(MatlabFunctionHandle.MatlabFunctionHandleGetter.class)
public final class MatlabFunctionHandle extends MatlabType
{
    private final String _function;
    
    public MatlabFunctionHandle(String functionName)
    {
        if(functionName == null)
        {
            throw new NullPointerException("MATLAB function name may not be null");
        }
        
        if(functionName.isEmpty())
        {
            throw new IllegalArgumentException("Invalid MATLAB function name; may not be an empty String");
        }

        //Anonymous function
        if(functionName.startsWith("@"))
        {
            checkAnonymousFunction(functionName);
        }
        //Regular function
        else
        {
            checkFunctionName(functionName);
        }
        
        _function = functionName;
    }
    
    private static void checkAnonymousFunction(String function)
    {
        char[] functionChars = function.toCharArray();
        
        //Validate that the the anonymous function is of the form @(something)
        if(functionChars.length < 2 || functionChars[1] != '(')
        {
            throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                    "@ must be followed with (");
        }
        int closingParenIndex = function.indexOf(")");
        if(closingParenIndex == -1)
        {
            throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                    "Must terminate the argument list with )");
        }

        //Validate all of the arguments of the anonymous function
        String argsString = function.substring(2, closingParenIndex);
        String[] args;
        args = argsString.isEmpty() ? new String[0] : argsString.split(", ");
        Set<String> seenArgs = new HashSet<String>();
        for(String arg : args)
        {
            arg = arg.trim();

            if(seenArgs.contains(arg))
            {
                throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                        "Invalid argument name: " + arg + "\n" +
                        "Argument names must be unique");
            }
            seenArgs.add(arg);

            char[] argsChars = arg.toCharArray();

            if(!Character.isLetter(argsChars[0]))
            {
                throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                        "Invalid argument name: " + arg + "\n" +
                        "Argument must begin with a letter");
            }

            for(char element : argsChars)
            {
                if(!(Character.isLetter(element) || Character.isDigit(element) || element == '_'))
                {
                    throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                            "Invalid argument name: " + arg + "\n" +
                            "Argument must consist only of letters, numbers, and underscores");
                }
            }
        }

        //Validate the function has a body, but don't actually validate the body
        String body = function.substring(closingParenIndex + 1, function.length());
        if(body.trim().isEmpty())
        {
            throw new IllegalArgumentException("Invalid anonymous MATLAB function: " + function + "\n" +
                    "Anonymous function must have a body");
        }
    }
    
    private static void checkFunctionName(String functionName)
    {
        char[] nameChars = functionName.toCharArray();
        
        if(!Character.isLetter(nameChars[0]))
        {
            throw new IllegalArgumentException("Invalid MATLAB function name: " + functionName + "\n" +
                    "Function name must begin with a letter");
        }

        for(char element : nameChars)
        {
            if(!(Character.isLetter(element) || Character.isDigit(element) || element == '_'))
            {
                throw new IllegalArgumentException("Invalid MATLAB function name: " + functionName + "\n" +
                        "Function name must consist only of letters, numbers, and underscores");
            }
        }
    }
    
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + ", functionName=" + _function + "]";
    }
    
    @Override
    MatlabTypeSerializedSetter getSerializedSetter()
    {
        return new MatlabFunctionHandlerSetter(_function);
    }
    
    private static class MatlabFunctionHandlerSetter implements MatlabTypeSerializedSetter
    {
        private final String _function;
        
        public MatlabFunctionHandlerSetter(String function)
        {
            _function = function;
        }
        
        @Override
        public void setInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException
        {   
            if(_function.startsWith("@"))
            {
                proxy.eval(variableName + " = " + _function + ";");
            }
            else
            {
                proxy.eval(variableName + " = @" + _function + ";");
            }
        }
    }
    
    static class MatlabFunctionHandleGetter implements MatlabTypeSerializedGetter
    {
        private String _function;
        private boolean _retrieved = false;
        
        @Override
        public MatlabFunctionHandle deserialize()
        {
            if(_retrieved)
            {
                return new MatlabFunctionHandle(_function);
            }
            else
            {
                throw new IllegalStateException("MatlabFunctionHandle cannot be deserialized until the data has been " +
                        "retrieved from MATLAB");
            }
        }

        @Override
        public void getInMatlab(MatlabThreadProxy proxy, String variableName) throws MatlabInvocationException
        {
            String type = (String) proxy.returningEval("class(" + variableName + ");", 1)[0];
            if(!type.equals("function_handle"))
            {
                throw new IncompatibleReturnException(variableName + " is of type " + type + " not function_handle");
            }
            
            _function = (String) proxy.returningEval("func2str(" + variableName + ");", 1)[0];
            
            _retrieved = true;
        }
    }   
}