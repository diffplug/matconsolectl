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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    
    /**************************************************************************************************************\
    |*                                        Linking & Validation                                                *|
    \**************************************************************************************************************/
    
    public static <T> T link(Class<T> functionInterface, MatlabProxy matlabProxy)
    {
        if(!functionInterface.isInterface())
        {
            throw new LinkingException(functionInterface.getName() + " is not an interface");
        }
        
        //Information about the functions
        Map<Method, ResolvedFunctionInfo> functionsInfo = new ConcurrentHashMap<Method, ResolvedFunctionInfo>();
        
        //Validate and retrieve information about all of the methods in the interface
        for(Method method : functionInterface.getMethods())
        {
            MatlabFunctionInfo annotation = method.getAnnotation(MatlabFunctionInfo.class);
            
            //Check that all invariants are held
            checkMethodAnnotation(method, annotation);
            checkMethodReturn(method, annotation);
            checkMethodExceptions(method);
            
            functionsInfo.put(method, resolveMatlabFunctionInfo(functionInterface, method, annotation));
        }
        
        T functionProxy = (T) Proxy.newProxyInstance(functionInterface.getClassLoader(),
                new Class<?>[] { functionInterface }, new MatlabFunctionInvocationHandler(matlabProxy, functionsInfo));
        
        return functionProxy;
    }
    
    private static ResolvedFunctionInfo resolveMatlabFunctionInfo(Class<?> functionInterface, Method method,
            MatlabFunctionInfo annotation)
    {
        String functionName;
        String containingDirectory;

        //If the name was specified, meaning the function is expected to be on MATLAB's path
        if(!annotation.name().isEmpty())
        {
            functionName = annotation.name();
            containingDirectory = null;
        }
        else
        {
            String path; //Only need for exception messages
            File mFile;
            
            if(!annotation.absolutePath().isEmpty())
            {
                path = annotation.absolutePath();
                
                mFile = new File(annotation.absolutePath());
            }
            else
            {
                path = annotation.relativePath();
                
                File interfaceLocation = getClassLocation(functionInterface);
                try
                {   
                    //If this line succeeds then, then the interface is inside of a jar, so the m-file is as well
                    JarFile jar = new JarFile(interfaceLocation);

                    try
                    {
                        JarEntry entry = jar.getJarEntry(annotation.relativePath());

                        if(entry == null)
                        {
                             throw new LinkingException("Unable to find m-file inside of jar\n" +
                                "method: " + method.getName() + "\n" +
                                "path: " + annotation.relativePath() + "\n" +
                                "jar location: " + interfaceLocation.getAbsolutePath());
                        }

                        String entryName = entry.getName();
                        if(!entryName.endsWith(".m"))
                        {
                            throw new LinkingException("Specified m-file does not end in .m\n" +
                                    "method: " + method.getName() + "\n" +
                                    "path: " + annotation.relativePath() + "\n" +
                                    "jar location: " + interfaceLocation.getAbsolutePath());
                        }

                        functionName = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length() - 2);
                        mFile = extractFromJar(jar, entry, functionName);
                        jar.close();
                    }
                    catch(IOException e)
                    {
                        throw new LinkingException("Unable to extract m-file from jar\n" +
                                "method: " + method.getName() + "\n" +
                                "path: " + annotation.relativePath() + "\n" +
                                "jar location: " + interfaceLocation, e);
                    }
                }
                //Interface is not located inside a jar, so neither is the m-file
                catch(IOException e)
                {
                    mFile = new File(interfaceLocation, annotation.relativePath());
                }
            }
            
            //Resolve canonical path
            try
            {
                mFile = mFile.getCanonicalFile();
            }
            catch(IOException ex)
            {
                throw new LinkingException("Unable to resolve canonical path of specified function\n" +
                        "method: " + method.getName() + "\n" +
                        "path:" + path + "\n" +
                        "non-canonical path: " + mFile.getAbsolutePath(), ex);
            }
            
            //Validate file location
            if(!mFile.exists())
            {
                throw new LinkingException("Specified m-file does not exist\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + mFile.getAbsolutePath());
            }
            if(!mFile.isFile())
            {
                throw new LinkingException("Specified m-file is not a file\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + mFile.getAbsolutePath());
            }
            if(!mFile.getName().endsWith(".m"))
            {
                throw new LinkingException("Specified m-file does not end in .m\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + mFile.getAbsolutePath());
            }
            
            //Parse out the name of the function and the directory containing it
            containingDirectory = mFile.getParent();
            functionName = mFile.getName().substring(0, mFile.getName().length() - 2); 
        }

        return new ResolvedFunctionInfo(functionName, annotation.nargout(), annotation.eval(), containingDirectory);
    }
    
    private static File extractFromJar(JarFile jar, JarEntry entry, String functionName) throws IOException
    {
        //Source
        InputStream entryStream = jar.getInputStream(entry);

        //Destination
        File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        File destFile = new File(tempDir, functionName + ".m");
        if(destFile.exists())
        {
            throw new IOException("Unable to extract m-file, randomly generated path already defined\n" +
                    "function: " + functionName + "\n" +
                    "generated path: " + destFile.getAbsolutePath());
        }
        destFile.getParentFile().mkdirs();
        destFile.deleteOnExit();

        //Copy source to destination
        final int BUFFER_SIZE = 2048;
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
        int count;
        while((count = entryStream.read(buffer, 0, BUFFER_SIZE)) != -1)
        {
           dest.write(buffer, 0, count);
        }
        dest.flush();
        dest.close();

        return destFile;
    }
    
    /**
     * A simple holder of information which closely matches {@link MatlabFunctionInfo}. However, it further resolves
     * the information provider by users of {@code MatlabFunctionInfo}.
     */
    private static class ResolvedFunctionInfo implements Serializable
    {
        /**
         * The name of the function.
         */
        final String name;
        
        /**
         * Number of return arguments.
         */
        final int nargout;
        
        /**
         * Whether {@code eval} instead of {@code feval} should be used.
         */
        final boolean eval;
        
        /**
         * The directory containing the function. Will be {@code null} if the function has been specified as being on
         * MATLAB's path.
         */
        final String containingDirectory;
        
        private ResolvedFunctionInfo(String name, int nargout, boolean eval, String containingDirectory)
        {
            this.name = name;
            this.nargout = nargout;
            this.eval = eval;
            this.containingDirectory = containingDirectory;
        }
    }
    
    private static void checkMethodAnnotation(Method method, MatlabFunctionInfo annotation)
    {
        if(annotation == null)
        {
            throw new LinkingException(method + " does not have a " + MatlabFunctionInfo.class.getName() +
                    " annotation.");
        }
        
        //Verify exactly one of name, absolutePath, and relativePath has been specified
        boolean hasName = !annotation.name().isEmpty();
        boolean hasAbsolutePath = !annotation.absolutePath().isEmpty();
        boolean hasRelativePath = !annotation.relativePath().isEmpty();
        if( (hasName && (hasAbsolutePath || hasRelativePath)) ||
            (hasAbsolutePath && (hasName || hasRelativePath)) ||
            (hasRelativePath && (hasAbsolutePath || hasName)) ||
            (!hasName && !hasAbsolutePath && !hasRelativePath) )
        {
            throw new LinkingException(method + "'s " + MatlabFunctionInfo.class.getName() + " annotation must " +
                    "specify either a function name, an absolute path, or a relative path. It must specify exactly " +
                    "one.");
        }
    }
    
    private static void checkMethodReturn(Method method, MatlabFunctionInfo annotation)
    {
        //Returned arguments must be 0 or greater
        if(annotation.nargout() < 0)
        {
            throw new LinkingException(method + "'s " + MatlabFunctionInfo.class.getName() + 
                    " annotation specifies a negative nargout of " + annotation.nargout() + ". nargout must be " +
                    " 0 or greater.");
        }

        //Validate return type & nargout info 
        Class<?> returnType = method.getReturnType();

        //If a return type is specified then nargout must be greater than 0
        if(!returnType.equals(Void.TYPE) && annotation.nargout() == 0)
        {
            throw new LinkingException(method + " has a non-void return type but does not " +
                    "specify the number of return arguments or specified 0.");
        }

        //If void return type then nargout must be 0
        if(returnType.equals(Void.TYPE) && annotation.nargout() != 0)
        {
            throw new LinkingException(method + " has a void return type but has a non-zero nargout [" +
                    annotation.nargout() + "] value");
        }

        //If multiple values are returned, the return type must be an array of objects
        if(annotation.nargout() > 1 &&
                (!returnType.isArray() || (returnType.isArray() && returnType.getComponentType().isPrimitive())))
        {
            throw new LinkingException(method + " must have a return type of an array of objects.");
        }

        //If eval then the only allowed argument is a single string
        if(annotation.eval())
        {
            Class<?>[] parameters = method.getParameterTypes();
            if(parameters.length != 1 || !parameters[0].equals(String.class))
            {
                throw new LinkingException(method + " must have String as its only parameter " +
                        "because the function will be invoked using eval.");
            }
        }
    }
    
    private static void checkMethodExceptions(Method method)
    {
        //Check the method throws MatlabInvocationException
        if(!Arrays.asList(method.getExceptionTypes()).contains(MatlabInvocationException.class))
        {
            throw new LinkingException(method.getName() + " must throw " + MatlabInvocationException.class);
        }
    }
    
    private static File getClassLocation(Class<?> clazz)
    {
        try
        {
            URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(url.toURI().getPath()).getCanonicalFile();
            
            return file;
        }
        catch(IOException e)
        {
            throw new LinkingException("Unable to determine location of " + clazz.getName(), e);
        }
        catch(URISyntaxException e)
        {
            throw new LinkingException("Unable to determine location of " + clazz.getName(), e);
        }
    }
    
    /**
     * Represents an issue linking a Java method to a MATLAB function.
     * 
     * @since 4.1.0
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static class LinkingException extends RuntimeException
    {
        private LinkingException(String msg)
        {
            super(msg);
        }
        
        private LinkingException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }
    
    
    /**************************************************************************************************************\
    |*                                        Function Invocation                                                 *|
    \**************************************************************************************************************/
    
    
    private static class MatlabFunctionInvocationHandler implements InvocationHandler
    {
        private final MatlabProxy _proxy;
        private final Map<Method, ResolvedFunctionInfo> _functionsInfo;
        
        private MatlabFunctionInvocationHandler(MatlabProxy proxy, Map<Method, ResolvedFunctionInfo> functionsInfo)
        {
            _proxy = proxy;
            _functionsInfo = functionsInfo;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws MatlabInvocationException
        {   
            ResolvedFunctionInfo functionInfo = _functionsInfo.get(method);
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
        private final ResolvedFunctionInfo _functionInfo;
        private final Object[] _args;
        
        private CustomFunctionInvocation(ResolvedFunctionInfo functionInfo, Object[] args)
        {
            _functionInfo = functionInfo;
            _args = args;
        }
        
        @Override
        public Object[] call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            String initialDir = null;
            
            //If the function was specified as not being on MATLAB's path
            if(_functionInfo.containingDirectory != null)
            {
                //Initial directory before cding
                initialDir = (String) proxy.returningFeval("pwd", 1)[0];
                
                //No need to change directory
                if(initialDir.equals(_functionInfo.containingDirectory))
                {
                    initialDir = null;
                }
                //Change directory to where the function is located
                else
                {
                    proxy.feval("cd", _functionInfo.containingDirectory);
                }
            }
            
            //Invoke function
            try
            {
                Object[] result;
                
                //If using eval
                if(_functionInfo.eval)
                {
                    String command = _functionInfo.name + "(" + _args[0] + ");";
                    
                    if(_functionInfo.nargout == 0)
                    {
                        proxy.eval(command);
                        result = null;
                    }
                    else
                    {
                        result = proxy.returningEval(command, _functionInfo.nargout);
                    }
                }
                //If using feval
                else
                {
                    if(_functionInfo.nargout == 0)
                    {
                        proxy.feval(_functionInfo.name, _args);
                        result = null;
                    }
                    else
                    {
                        result = proxy.returningFeval(_functionInfo.name, _functionInfo.nargout, _args);
                    }
                }
            
                return result;
            }
            //If necessary, change back to the directory MATLAB was in before the function was invoked
            finally
            {
                if(initialDir != null)
                {
                    proxy.feval("cd", initialDir);
                }
            }
        }     
    }
    
    public static class IncompatibleReturnException extends RuntimeException
    {
        private IncompatibleReturnException(String msg)
        {
            super(msg);
        }
    }
}