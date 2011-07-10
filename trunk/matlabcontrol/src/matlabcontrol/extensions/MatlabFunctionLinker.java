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

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

/**
 *
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabFunctionLinker
{
    private MatlabFunctionLinker() { }
    
    public static <T> T link(Class<T> functionInterface, MatlabProxy matlabProxy)
    {
        if(!functionInterface.isInterface())
        {
            throw new LinkingException(functionInterface.getName() + " is not an interface");
        }
        
        //Validate all of the methods in the interface
        for(Method method : functionInterface.getMethods())
        {
            MatlabFunctionInfo functionInfo = method.getAnnotation(MatlabFunctionInfo.class);
            
            //Check method is annotated
            if(functionInfo == null)
            {
                throw new LinkingException(method + " is not annotated with a " +
                        MatlabFunctionInfo.class.getName() + ". All methods defined or inherited in " + 
                        functionInterface.getName() + " must be annotated with " + MatlabFunctionInfo.class.getName());
            }
            
            //Validate path & name info
            if(functionInfo.path().isEmpty())
            {
                if(functionInfo.name().isEmpty())
                {
                    throw new LinkingException(method + "'s " + MatlabFunctionInfo.class.getName() + " must specify " +
                            "either a name or a path.");
                }
            }
            else
            {
                if(!functionInfo.name().isEmpty())
                {
                    throw new LinkingException(method + "'s " + MatlabFunctionInfo.class.getName() + " must specify " +
                            "either a name or a path, not both.");
                }
                
                File file = new File(functionInfo.path());
                if(!file.exists())
                {
                    throw new LinkingException("Specified location of m-file does not exist: " + functionInfo.path());
                }
                
                if(!file.isFile())
                {
                    throw new LinkingException("Specified  m-file is not a file: " + functionInfo.path());
                }
                
                if(!file.getName().endsWith(".m"))
                {
                    throw new LinkingException("Specified m-file does not end with .m: " + functionInfo.path());
                }
            }
            
            //Returned arguments must be 0 or greater
            if(functionInfo.nargout() < 0)
            {
                throw new LinkingException(method + "'s " + MatlabFunctionInfo.class.getName() + 
                        " annotation specifies a negative nargout [" + functionInfo.nargout() + "]. nargout must be " +
                        " 0 or greater.");
            }
            
            //Validate return type & nargout info 
            Class<?> returnType = method.getReturnType();
            
            //If a return type is specified then nargout must be greater than 0
            if(!returnType.equals(Void.TYPE) && functionInfo.nargout() == 0)
            {
                throw new LinkingException(method + " has a non-void return type but does not " +
                        "specify the number of return arguments or specified 0.");
            }
            
            //If void return type then nargout must be 0
            if(returnType.equals(Void.TYPE) && functionInfo.nargout() != 0)
            {
                throw new LinkingException(method + " has a void return type but has a non-zero nargout [" +
                        functionInfo.nargout() + "] value");
            }
            
            //If multiple values are returned, the return type must be an array of objects
            if(functionInfo.nargout() > 1 &&
                    (!returnType.isArray() || (returnType.isArray() && returnType.getComponentType().isPrimitive())))
            {
                throw new LinkingException(method + " must have a return type of an array of objects.");
            }
            
            //If eval then the only allowed argument is a Single string
            if(functionInfo.eval())
            {
                Class<?>[] parameters = method.getParameterTypes();
                if(parameters.length != 1 || !parameters[0].equals(String.class))
                {
                    throw new LinkingException(method + " must have String as its only parameter " +
                            "because the function will be invoked using eval.");
                }
            }
            
            //Check the method throws MatlabInvocationException
            if(!Arrays.asList(method.getExceptionTypes()).contains(MatlabInvocationException.class))
            {
                throw new LinkingException(method.getName() + " must throw " +
                        MatlabInvocationException.class);
            }
        }
        
        T functionProxy = (T) Proxy.newProxyInstance(functionInterface.getClassLoader(),
                new Class<?>[] { functionInterface }, new MatlabFunctionInvocationHandler(matlabProxy));
        
        return functionProxy;
    }
    
    private static class MatlabFunctionInvocationHandler implements InvocationHandler
    {
        private final MatlabProxy _proxy;
        
        private MatlabFunctionInvocationHandler(MatlabProxy proxy)
        {
            _proxy = proxy;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws MatlabInvocationException
        {
            MatlabFunctionInfo functionInfo = method.getAnnotation(MatlabFunctionInfo.class);
            Object[] functionResult = _proxy.invokeAndWait(new CustomFunctionInvocation(functionInfo, args));
            
            Object result;
            
            //If the method is using return type inference
            Class<?> returnType = method.getReturnType();
            if(!returnType.equals(Object[].class))
            {
                result = convertReturnType(functionResult, returnType);
            }
            else
            {
                result = functionResult;
            }
            
            return result;
        }
        
        private Object convertReturnType(Object[] result, Class<?> returnType)
        {
            Object toReturn;
            
            if(result.length == 1)
            {
                toReturn = convertSingleReturn(result[0], returnType);
            }
            else
            {
                toReturn = convertMultipleReturn(result, returnType);
            }
            
            return toReturn;
        }
        
        private Object convertMultipleReturn(Object[] values, Class<?> returnType)
        {
            //Determine if each element of values can be assigned to the component type of returnType
            for(Object value : values)
            {
                if(value != null && !returnType.getComponentType().isAssignableFrom(value.getClass()))
                {
                    throw new IncompatibleReturnException("Required return type " + returnType.getCanonicalName() +
                            " requires that each returned argument be assignable to "  + 
                            returnType.getComponentType().getCanonicalName() + ". Argument type " + 
                            value.getClass().getCanonicalName() + " returned by MATLAB is unassignable.");
                }
            }
            
            //Build an array of the return type's component type, place the values in it
            Object newValuesArray = Array.newInstance(returnType.getComponentType(), values.length);
            for(int i = 0; i < values.length; i++)
            {
                Array.set(newValuesArray, i, values[i]);
            }
            
            return newValuesArray;
        }
        
        private Object convertSingleReturn(Object value, Class<?> returnType)
        {
            Object toReturn;
            if(value == null)
            {
                toReturn = null;
            }
            else if(returnType.isPrimitive())
            {
                toReturn = convertPrimitiveReturnType(value, returnType);
            }
            else
            {
                if(!returnType.isAssignableFrom(value.getClass()))
                {
                    throw new IncompatibleReturnException("Required return type [" + returnType.getCanonicalName() +
                            "] is incompatible with type returned from MATLAB [" + value.getClass().getCanonicalName() +
                            "].");
                }
                
                toReturn = value;
            }
            
            return toReturn;
        }

        private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_AUTOBOXED = new ConcurrentHashMap<Class<?>, Class<?>>();
        static
        {
            PRIMITIVE_TO_AUTOBOXED.put(byte.class, Byte.class);
            PRIMITIVE_TO_AUTOBOXED.put(short.class, Short.class);
            PRIMITIVE_TO_AUTOBOXED.put(int.class, Integer.class);
            PRIMITIVE_TO_AUTOBOXED.put(long.class, Long.class);
            PRIMITIVE_TO_AUTOBOXED.put(double.class, Double.class);
            PRIMITIVE_TO_AUTOBOXED.put(float.class, Float.class);
            PRIMITIVE_TO_AUTOBOXED.put(boolean.class, Boolean.class);
            PRIMITIVE_TO_AUTOBOXED.put(char.class, Character.class);
        }
        
        private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_ARRAY_OF = new ConcurrentHashMap<Class<?>, Class<?>>();
        static
        {
            PRIMITIVE_TO_ARRAY_OF.put(byte.class, byte[].class);
            PRIMITIVE_TO_ARRAY_OF.put(short.class, short[].class);
            PRIMITIVE_TO_ARRAY_OF.put(int.class, int[].class);
            PRIMITIVE_TO_ARRAY_OF.put(long.class, long[].class);
            PRIMITIVE_TO_ARRAY_OF.put(double.class, double[].class);
            PRIMITIVE_TO_ARRAY_OF.put(float.class, float[].class);
            PRIMITIVE_TO_ARRAY_OF.put(boolean.class, boolean[].class);
            PRIMITIVE_TO_ARRAY_OF.put(char.class, char[].class);
        }
        
        private Object convertPrimitiveReturnType(Object value, Class<?> returnType)
        {
            Class<?> actualType = value.getClass();
            
            Class<?> autoBoxOfReturnType = PRIMITIVE_TO_AUTOBOXED.get(returnType);
            Class<?> arrayOfReturnType = PRIMITIVE_TO_ARRAY_OF.get(returnType);
            
            Object result;
            if(actualType.equals(autoBoxOfReturnType))
            {
                result = value;
            }
            else if(actualType.equals(arrayOfReturnType))
            {
                if(Array.getLength(value) != 1)
                {
                    throw new IncompatibleReturnException("Array of " + returnType.getCanonicalName() + " does not " +
                                "have exactly 1 value.");
                }
                
                result = Array.get(value, 0);
            }
            else
            {
                throw new IncompatibleReturnException("Required return type [" + returnType.getCanonicalName() + "] " +
                        "is incompatible with the type returned from MATLAB [" + actualType.getCanonicalName() + "].");
            }
            
            return result;
        }
    }
    
    private static class CustomFunctionInvocation implements MatlabProxy.MatlabThreadCallable<Object[]>, Serializable
    {
        private final MatlabFunctionInfo _functionInfo;
        private final Object[] _args;
        
        private CustomFunctionInvocation(MatlabFunctionInfo functionInfo, Object[] args)
        {
            _functionInfo = functionInfo;
            _args = args;
        }
        
        @Override
        public Object[] call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            String initialDir = null;
            String functionName;
            
            //If no path was specified, meaning the function is expected to be on MATLAB's path
            if(_functionInfo.path().isEmpty())
            {
                functionName = _functionInfo.name();
            }
            else
            {
                //Function name is the file name without the terminating '.m'
                File file = new File(_functionInfo.path());
                functionName = file.getName().substring(0, file.getName().length() - 2); 
                
                //Change directory to where the function is located
                initialDir = (String) proxy.returningFeval("pwd", 1)[0];
                File path = new File(_functionInfo.path());
                proxy.feval("cd", path.getParent());
            }
            
            //Invoke function
            try
            {
                Object[] result;
                
                //If using eval
                if(_functionInfo.eval())
                {
                    String command = _functionInfo.name() + "(" + _args[0] + ");";
                    
                    if(_functionInfo.nargout() == 0)
                    {
                        proxy.eval(command);
                        result = null;
                    }
                    else
                    {
                        result = proxy.returningEval(command, _functionInfo.nargout());
                    }
                }
                //If using feval
                else
                {
                    if(_functionInfo.nargout() == 0)
                    {
                        proxy.feval(_functionInfo.name(), _args);
                        result = null;
                    }
                    else
                    {
                        result = proxy.returningFeval(functionName, _functionInfo.nargout(), _args);
                    }
                }
            
                return result;
            }
            //Change back to the directory MATLAB was in before the function was invoked
            finally
            {
                if(initialDir != null)
                {
                    proxy.feval("cd", initialDir);
                }
            }
        }     
    }
    
    public static class LinkingException extends RuntimeException
    {
        private LinkingException(String msg)
        {
            super(msg);
        }
    }
    
    private static class IncompatibleReturnException extends RuntimeException
    {
        private IncompatibleReturnException(String msg)
        {
            super(msg);
        }
    }
}