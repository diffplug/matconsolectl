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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.link.ArrayMultidimensionalizer.PrimitiveArrayGetter;
import matlabcontrol.link.ComplexNumber.ComplexNumberGetter;
import matlabcontrol.link.MatlabFunctionHandle.MatlabFunctionHandleGetter;
import matlabcontrol.link.MatlabNumericArray.MatlabNumericArrayGetter;
import matlabcontrol.link.MatlabReturns.ReturnN;
import matlabcontrol.link.MatlabType.MatlabTypeGetter;
import matlabcontrol.link.MatlabType.MatlabTypeSetter;

/**
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabFunctionLinker
{    
    private MatlabFunctionLinker() { }
    
    
    /**************************************************************************************************************\
    |*                                        Linking & Validation                                                *|
    \**************************************************************************************************************/
    
    
    public static <T> T link(Class<T> functionInterface, MatlabProxy proxy)
    {
        if(!functionInterface.isInterface())
        {
            throw new LinkingException(functionInterface.getCanonicalName() + " is not an interface");
        }
        
        if(proxy == null)
        {
            throw new NullPointerException("proxy may not be null");
        }
        
        //Maps each method to information about the function and method
        ConcurrentMap<Method, InvocationInfo> resolvedInfo = new ConcurrentHashMap<Method, InvocationInfo>();
        
        //Validate and retrieve information about all of the methods in the interface
        for(Method method : functionInterface.getMethods())
        {
            //Check method is annotated with function information
            MatlabFunctionInfo annotation = method.getAnnotation(MatlabFunctionInfo.class);
            if(annotation == null)
            {
                throw new LinkingException(method + " is not annotated with " + 
                        MatlabFunctionInfo.class.getCanonicalName());
            }
            
            //Check method can throw MatlabInvocationException
            checkExceptions(method);
            
            //Store information about how to invoke the method, performing validation in the process
            resolvedInfo.put(method, getInvocationInfo(method, annotation));
        }
        
        T functionProxy = (T) Proxy.newProxyInstance(functionInterface.getClassLoader(),
                new Class<?>[] { functionInterface },
                new MatlabFunctionInvocationHandler(proxy, functionInterface, resolvedInfo));
        
        return functionProxy;
    }
    
    private static void checkExceptions(Method method)
    {
        boolean assignable = false;
        Type[] genericExceptions = method.getGenericExceptionTypes();
        for(Type exception : genericExceptions)
        {
            if(exception instanceof Class && ((Class<?>) exception).isAssignableFrom(MatlabInvocationException.class))
            {
                assignable = true;
            }
        }
        
        if(!assignable)
        {
            throw new LinkingException(method + " is not capable of throwing " +
                    MatlabInvocationException.class.getCanonicalName() + " or does so with generics");
        }
    }
    
    private static class FunctionInfo
    {
        final String name;
        final String containingDirectory;
        
        public FunctionInfo(String name, String containingDirectory)
        {
            this.name = name;
            this.containingDirectory = containingDirectory;
        }
    }
    
    private static FunctionInfo getFunctionInfo(Method method, MatlabFunctionInfo annotation)
    {
        //Determine the function's name and if applicable, the directory the function is located in
        String functionName;
        String containingDirectory;
        
        //If a function name
        if(isFunctionName(annotation.value()))
        {
            functionName = annotation.value();
            containingDirectory = null;
        }
        //If a path
        else
        {   
            String path = annotation.value();
            File mFile;
            
            //If an absolute path
            if(new File(path).isAbsolute())
            {
                mFile = new File(path);
            }
            //If a relative path
            else
            {
                File interfaceLocation = getClassLocation(method.getDeclaringClass());
                try
                {   
                    //If this line succeeds then, then the interface is inside of a zip file, so the m-file is as well
                    ZipFile zip = new ZipFile(interfaceLocation);

                    ZipEntry entry = zip.getEntry(path);

                    if(entry == null)
                    {
                         throw new LinkingException("Unable to find m-file inside of jar\n" +
                            "method: " + method.getName() + "\n" +
                            "path: " + path + "\n" +
                            "zip file location: " + interfaceLocation.getAbsolutePath());
                    }

                    String entryName = entry.getName();
                    if(!entryName.endsWith(".m"))
                    {
                        throw new LinkingException("Specified m-file does not end in .m\n" +
                                "method: " + method.getName() + "\n" +
                                "path: " + path + "\n" +
                                "zip file location: " + interfaceLocation.getAbsolutePath());
                    }

                    functionName = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length() - 2);
                    mFile = extractFromZip(zip, entry, functionName, method, interfaceLocation, annotation);
                    zip.close();
                }
                //Interface is not located inside a jar, so neither is the m-file
                catch(IOException e)
                {
                    mFile = new File(interfaceLocation, path);
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
        
        return new FunctionInfo(functionName, containingDirectory);
    }
    
    private static boolean isFunctionName(String functionName)
    {
        boolean isFunctionName = true;
        
        char[] nameChars = functionName.toCharArray();
        
        if(!Character.isLetter(nameChars[0]))
        {
            isFunctionName = false;
        }
        else
        {
            for(char element : nameChars)
            {
                if(!(Character.isLetter(element) || Character.isDigit(element) || element == '_'))
                {
                    isFunctionName = false;
                    break;
                }
            }
        }
        
        return isFunctionName;
    }
    
    /**
     * Extracts the {@code entry} belonging to {@code zip} to a file named {@code functionName}.m that is placed in
     * the directory specified by the property {@code java.io.tmpdir}. It is not placed directly in that directory, but
     * instead inside a directory with a randomly generated name. The file is deleted upon JVM termination.
     * 
     * @param zip
     * @param entry
     * @param functionName
     * @param method  used only for exception message
     * @param interfaceLocation  used only for exception message
     * @param annotation   used only for exception message
     * @return
     * @throws LinkingException 
     */
    private static File extractFromZip(ZipFile zip, ZipEntry entry, String functionName,
            Method method, File interfaceLocation, MatlabFunctionInfo annotation)
    {
        try
        {
            //Source
            InputStream entryStream = zip.getInputStream(entry);

            //Destination
            File tempDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
            File destFile = new File(tempDir, functionName + ".m");
            if(destFile.exists())
            {
                throw new LinkingException("Unable to extract m-file, randomly generated path already defined\n" +
                        "method: " + method + "\n" +
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
        catch(IOException e)
        {
            throw new LinkingException("Unable to extract m-file from jar\n" +
                "method: " + method.getName() + "\n" +
                "path: " + annotation.value() + "\n" +
                "zip file location: " + interfaceLocation.getAbsolutePath(), e);
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
    
    private static Class<?>[] getReturnTypes(Method method)
    {
        //The type-erasured return type of the method
        Class<?> methodReturn = method.getReturnType();
        
        //The return type of the method with type information
        Type genericReturn = method.getGenericReturnType();
        
        //The return types to be determined
        Class<?>[] returnTypes;
        
        //0 return arguments
        if(methodReturn.equals(void.class))
        {
            returnTypes = new Class<?>[0];
        }
        //1 return argument
        else if(!ReturnN.class.isAssignableFrom(methodReturn))
        {
            if(!methodReturn.equals(genericReturn))
            {
                throw new LinkingException(method + " may not have a return type that uses generics");
            }
            
            returnTypes = new Class<?>[]{ methodReturn };
        }
        //2 or more return arguments
        else
        {   
            if(genericReturn instanceof ParameterizedType)
            {
                Type[] parameterizedTypes = ((ParameterizedType) genericReturn).getActualTypeArguments();
                returnTypes = new Class<?>[parameterizedTypes.length];
                
                for(int i = 0; i < parameterizedTypes.length; i++)
                {
                    Type type = parameterizedTypes[i];
                
                    if(type instanceof Class)
                    {
                        returnTypes[i] = (Class) type;
                    }
                    else if(type instanceof GenericArrayType)
                    {   
                        returnTypes[i] = getClassOfArrayType((GenericArrayType) type, method);
                    }
                    else if(type instanceof ParameterizedType)
                    {
                        throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
                                " with a parameterized type");
                    }
                    else if(type instanceof WildcardType)
                    {
                        throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
                                " with a wild card type");
                    }
                    else if(type instanceof TypeVariable)
                    {
                        throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
                                " with a generic type");
                    }
                    else
                    {
                        throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
                                " with " + type);
                    }
                }
            }
            else
            {
                throw new LinkingException(method + " must parameterize " + methodReturn.getCanonicalName());
            }
        }
        
        return returnTypes;
    }
    
    /**
     * 
     * @param type
     * @param method used for exception message only
     * @return 
     */
    private static Class<?> getClassOfArrayType(GenericArrayType type, Method method)
    {
        int dimensions = 1;
        Type componentType = type.getGenericComponentType();
        while(!(componentType instanceof Class))
        {
            dimensions++;
            if(componentType instanceof GenericArrayType)
            {
                componentType = ((GenericArrayType) componentType).getGenericComponentType();
            }
            else if(componentType instanceof TypeVariable)
            {
                throw new LinkingException(method + " may not parameterize " + 
                        method.getReturnType().getCanonicalName() + " with a generic array");
            }
            else
            {
                throw new LinkingException(method + " may not parameterize " + 
                        method.getReturnType().getCanonicalName() + " with an array of type " + type);
            }
        }
        
        return ArrayTransformUtils.getMultidimensionalArrayClass((Class<?>) componentType, dimensions);
    }
    
    private static InvocationInfo getInvocationInfo(Method method, MatlabFunctionInfo annotation)
    {   
        FunctionInfo funcInfo = getFunctionInfo(method, annotation);
        Class<?>[] returnTypes = getReturnTypes(method);
        InvocationInfo invocationInfo = new InvocationInfo(funcInfo.name, funcInfo.containingDirectory, returnTypes);
            
        return invocationInfo;
    }
    
    /**
     * A holder of information about the Java method and the associated MATLAB function.
     */
    private static class InvocationInfo implements Serializable
    {
        /**
         * The name of the function.
         */
        final String name;
        
        /**
         * The directory containing the function. This is an absolute path to an m-file on the system, never inside of
         * a compressed file such as a jar. {@code null} if the function is (supposed to be) on MATLAB's path.
         */
        final String containingDirectory;
        
        /**
         * The types of each returned argument. The length of this array is the nargout to call the function with.
         */
        final Class<?>[] returnTypes;
        
        private InvocationInfo(String name, String containingDirectory, Class<?>[] returnTypes)
        {
            this.name = name;
            this.containingDirectory = containingDirectory;
            this.returnTypes = returnTypes;
        }
        
        @Override
        public String toString()
        {
            return "[" + this.getClass().getName() + 
                    " name=" + name + "," +
                    " containingDirectory=" + containingDirectory + "," +
                    " returnTypes=" + Arrays.toString(returnTypes) + "]";
        }
    }
    
    private static class ClassInfo
    {
        private static ConcurrentMap<Class, ClassInfo> CACHE = new ConcurrentHashMap<Class, ClassInfo>();
        
        public static ClassInfo getInfo(Class<?> clazz)
        {
            ClassInfo info = CACHE.get(clazz);
            if(info == null)
            {
                info = new ClassInfo(clazz);
                CACHE.put(clazz, info);
            }
            
            return info;
        }
        
        /**
         * The class this information is about
         */
        final Class<?> theClass;
        
        /**
         * If the class is either {@code void} or {@code java.lang.Void}
         */
        final boolean isVoid;
        
        /**
         * If the class is primitive
         */
        final boolean isPrimitive;
        
        /**
         * If an array type
         */
        final boolean isArray;
        
        /**
         * If the array's base component type is a primitive
         */
        final boolean isPrimitiveArray;
        
        /**
         * If the base component type of an array, {@code null} if not an array
         */
        final Class<?> baseComponentType;
        
        /**
         * The number of array dimensions, {@code 0} if not an array
         */
        final int arrayDimensions;
        
        /**
         * If the class inherits from {@code MatlabType}
         */
        final boolean isMatlabType;
        
        private ClassInfo(Class<?> clazz)
        {
            theClass = clazz;
            
            isPrimitive = clazz.isPrimitive();
            
            if(clazz.isArray())
            {
                isArray = true;
                
                int dim = 0;
                Class type = clazz;
                while(type.isArray())
                {
                    dim++;
                    type = type.getComponentType();
                }
                
                arrayDimensions = dim;
                baseComponentType = type;
                isPrimitiveArray = type.isPrimitive();
            }
            else
            {
                isArray = false;
                baseComponentType = null;
                isPrimitiveArray = false;
                arrayDimensions = 0;
            }
            
            isVoid = clazz.equals(Void.class) || clazz.equals(void.class);
            isMatlabType = MatlabType.class.isAssignableFrom(clazz);
        }
    }

    
    /**************************************************************************************************************\
    |*                                        Function Invocation                                                 *|
    \**************************************************************************************************************/
    

    private static class MatlabFunctionInvocationHandler implements InvocationHandler
    {
        private final MatlabProxy _proxy;
        private final Class<?> _interface;
        private final ConcurrentMap<Method, InvocationInfo> _invocationsInfo;
        
        private MatlabFunctionInvocationHandler(MatlabProxy proxy, Class<?> functionInterface,
                ConcurrentMap<Method, InvocationInfo> functionsInfo)
        {
            _proxy = proxy;
            _interface = functionInterface;
            _invocationsInfo = functionsInfo;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws MatlabInvocationException
        {   
            Object result;
            
            //Method belongs to java.lang.Object
            if(method.getDeclaringClass().equals(Object.class))
            {
                result = invokeObjectMethod(o, method, args);
            }
            //Method belongs to the interface supplied to this linker
            else
            {
                result = invokeMatlabFunction(o, method, args);
            }
            
            return result;
        }
        
        private Object invokeObjectMethod(Object o, Method method, Object[] args)
        {
            Object result;
            
            //public void String toString()
            if(method.getName().equals("toString") && method.getReturnType().equals(String.class) && 
                    method.getParameterTypes().length == 0)
            {
                result = "[" + _interface.getCanonicalName() + " info=" + _invocationsInfo + "]";
            }
            //public boolean equals(Object other)
            else if(method.getName().equals("equals") && method.getReturnType().equals(boolean.class) &&
                    method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Object.class))
            {
                Object other = args[0];
                if(other == null || !Proxy.isProxyClass(other.getClass()))
                {
                    result = false;
                }
                else
                {
                    InvocationHandler handler = Proxy.getInvocationHandler(o);
                    if(handler instanceof MatlabFunctionInvocationHandler)
                    {
                        result = _interface.equals(((MatlabFunctionInvocationHandler) handler)._interface);
                    }
                    else
                    {
                        result = false;
                    }
                }
            }
            //public int hashCode()
            else if(method.getName().equals("hashCode") && method.getReturnType().equals(int.class) &&
                    method.getParameterTypes().length == 0)
            {
                result = _interface.hashCode();
            }
            //The other methods of java.lang.Object should not be sent to this invocation handler
            else
            {
                throw new UnsupportedOperationException(method + " not supported");
            }
            
            return result;
        }
        
        private Object invokeMatlabFunction(Object o, Method method, Object[] args) throws MatlabInvocationException
        {
            InvocationInfo info = _invocationsInfo.get(method);
            
            if(args == null)
            {
                args = new Object[0];
            }
            
            //If the method uses var args then we need to make those arguments first level
            if(method.isVarArgs() && args.length > 0)
            {   
                Object varArgs = args[args.length - 1];
                if(varArgs.getClass().isArray())
                {
                    //Number of variable arguments
                    int varArgLength = Array.getLength(varArgs);
                    
                    //Combined arguments, ignoring the last argument which was the var arg array container
                    Object[] allArgs = new Object[args.length + varArgLength - 1];
                    System.arraycopy(args, 0, allArgs, 0, args.length - 1);
                    
                    //Update information with var args as first level arguments
                    for(int i = 0; i < varArgLength; i++)
                    {
                        Object arg = Array.get(varArgs, i);
                        allArgs[i + args.length - 1] = arg;
                    }
                    args = allArgs;
                }
            }
            
            //Replace all arguments with parameters of a MatlabType subclass or a multidimensional primitive array with
            //their serialized setters
            for(int i = 0; i < args.length; i++)
            {
                if(args[i] != null)
                {
                    ClassInfo argInfo = ClassInfo.getInfo(args[i].getClass());

                    if(argInfo.isMatlabType)
                    {
                        args[i] = ((MatlabType) args[i]).getSetter();
                    }
                    else if(argInfo.isPrimitiveArray)
                    {
                        args[i] = ArrayLinearizer.getSerializedSetter(args[i]);
                    }
                }                
            }

            //Invoke function
            Object[] returnValues = _proxy.invokeAndWait(new CustomFunctionInvocation(info, args));

            //For each returned value that is a serialized getter, retrieve it
            for(int i = 0; i < returnValues.length; i++)
            {
                if(returnValues[i] instanceof MatlabTypeGetter)
                {
                    try
                    {
                        returnValues[i] = ((MatlabType.MatlabTypeGetter) returnValues[i]).retrieve();
                    }
                    catch(IncompatibleReturnException e)
                    {
                        throw new IncompatibleReturnException("Required return type is incompatible with the type " +
                                "actually returned\n" +
                                "Required Type: " + info.returnTypes[i].getCanonicalName(), e);
                    }
                }
            }
            
            return processReturnValues(returnValues, info.returnTypes, method.getReturnType()); 
        }
        
        private Object processReturnValues(Object[] result, Class<?>[] returnTypes, Class<?> methodReturn)
        {
            Object toReturn;
            if(result.length == 0)
            {
                toReturn = result;
            }
            else if(result.length == 1)
            {
                toReturn = validateReturnCompatability(result[0], returnTypes[0]);
            }
            //Return type is a subclass of ReturnN
            else
            {
                for(int i = 0; i < result.length; i++)
                {
                    result[i] = validateReturnCompatability(result[i], returnTypes[i]);
                }
                
                try
                {
                    toReturn = methodReturn.getDeclaredConstructor(Object[].class).newInstance(new Object[]{ result });
                }
                catch(IllegalAccessException e)
                {
                    throw new IncompatibleReturnException("Unable to create " + methodReturn.getCanonicalName(), e);
                }
                catch(InstantiationException e)
                {
                    throw new IncompatibleReturnException("Unable to create " + methodReturn.getCanonicalName(), e);
                }
                catch(InvocationTargetException e)
                {
                    throw new IncompatibleReturnException("Unable to create " + methodReturn.getCanonicalName(), e);
                }
                catch(NoSuchMethodException e)
                {
                    throw new IncompatibleReturnException("Unable to create " + methodReturn.getCanonicalName(), e);
                }
            }
            
            return toReturn;
        }
        
        private Object validateReturnCompatability(Object value, Class<?> returnType)
        {   
            Object toReturn;
            if(value == null)
            {
                if(returnType.isPrimitive())
                {
                    throw new IncompatibleReturnException("Required return type is a primitive which is incompatible " +
                            "with the return value of null\n" +
                            "Required type: " + returnType.getCanonicalName() + "\n" +
                            "Returned value: null");
                }
                else
                {
                    toReturn = null;
                }
            }
            else if(returnType.isPrimitive())
            {
                Class<?> autoboxedClass = PRIMITIVE_TO_AUTOBOXED.get(returnType);
                if(autoboxedClass.equals(value.getClass()))
                {
                    toReturn = value;
                }
                else
                {
                    throw new IncompatibleReturnException("Required return type is incompatible with the type " +
                            "actually returned\n" + 
                            "Required types: " + returnType.getCanonicalName() + " OR " +
                                                 autoboxedClass.getCanonicalName() + "\n" + 
                            "Returned type: " + value.getClass().getCanonicalName());
                }
            }
            else
            {
                if(!returnType.isAssignableFrom(value.getClass()))
                {
                    throw new IncompatibleReturnException("Required return type is incompatible with the type " +
                            "actually returned\n" +
                            "Required type: " + returnType.getCanonicalName() + "\n" +
                            "Returned type: " + value.getClass().getCanonicalName());
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
    }
    
    private static class CustomFunctionInvocation implements MatlabProxy.MatlabThreadCallable<Object[]>, Serializable
    {
        private final InvocationInfo _functionInfo;
        private final Object[] _args;
        
        private CustomFunctionInvocation(InvocationInfo functionInfo, Object[] args)
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
            
            List<String> usedNames = new ArrayList<String>();
            try
            {
                //Set all arguments as MATLAB variables and build a function call using those variables
                String functionStr = _functionInfo.name + "(";
                List<String> parameterNames = generateNames(proxy, "param_", _args.length);
                for(int i = 0; i < _args.length; i++)
                {
                    String name = parameterNames.get(i);
                    usedNames.add(name);
                    
                    setReturnValue(proxy, name, _args[i]);
                    
                    functionStr += name;
                    if(i != _args.length - 1)
                    {
                        functionStr += ", ";
                    }
                }
                functionStr += ");";
                
                //Return arguments
                List<String> returnNames = null;
                if(_functionInfo.returnTypes.length != 0)
                {
                    returnNames = generateNames(proxy, "return_", _functionInfo.returnTypes.length);
                    String returnStr = "[";
                    for(int i = 0; i < returnNames.size(); i++)
                    {
                        ClassInfo returnInfo = ClassInfo.getInfo(_functionInfo.returnTypes[i]);
                        
                        String name;
                        if(returnInfo.isVoid)
                        {
                            name = "~";
                        }
                        else
                        {
                            name = returnNames.get(i);
                            usedNames.add(name);
                        }
                        
                        returnStr += name;
                        if(i != returnNames.size() - 1)
                        {
                            returnStr += ", ";
                        }
                    }
                    returnStr += "]";
                    
                    functionStr = returnStr + " = " + functionStr;
                }
                
                //Invoke the function
                proxy.eval(functionStr);
                
                //Get the return values
                Object[] returnValues = new Object[_functionInfo.returnTypes.length];
                for(int i = 0; i < returnValues.length; i++)
                {
                    ClassInfo returnInfo = ClassInfo.getInfo(_functionInfo.returnTypes[i]);
                    
                    if(!returnInfo.isVoid)
                    {
                        returnValues[i] = getReturnValue(proxy, returnNames.get(i), returnInfo, usedNames);
                    }
                }
                
                return returnValues;
            }
            //Restore MATLAB's state to what it was before the function call happened
            finally
            {
                try
                {
                    //Clear all variables used
                    if(!usedNames.isEmpty())
                    {
                        String clearCmd = "clear ";
                        for(int i = 0; i < usedNames.size(); i++)
                        {
                            clearCmd += usedNames.get(i);

                            if(i != usedNames.size() - 1)
                            {
                                clearCmd += " ";
                            }
                        }
                        proxy.eval(clearCmd);
                    }
                }
                finally
                {
                    //If necessary, change back to the directory MATLAB was in before the function was invoked
                    if(initialDir != null)
                    {
                        proxy.feval("cd", initialDir);
                    }
                }
            }
        }
        
        private static void setReturnValue(MatlabThreadProxy proxy, String name, Object arg)
                throws MatlabInvocationException
        {
            if(arg instanceof MatlabTypeSetter)
            {
                ((MatlabTypeSetter) arg).setInMatlab(proxy, name);
            }
            else if(arg instanceof Number)
            {   
                Number number = (Number) arg;
                
                if(number instanceof Byte)
                {
                    proxy.eval(name + "=int8(" + number.byteValue() + ");");
                }
                else if(number instanceof Short)
                {
                    proxy.eval(name + "=int16(" + number.shortValue() + ");");
                }
                else if(number instanceof Integer)
                {
                    proxy.eval(name + "=int32(" + number.intValue() + ");");
                }
                else if(number instanceof Long)
                {
                    proxy.eval(name + "=int64(" + number.longValue() + ");");
                }
                else if(number instanceof Float)
                {
                    proxy.setVariable(name, new float[] { number.floatValue() });
                }
                else if(number instanceof Double)
                {
                    proxy.setVariable(name, new double[] { number.doubleValue() });
                }
                //Otherwise this Number subclass is not to be treated specially, set it as any other Java object
                else
                {
                    MatlabValueSetter.setValue(proxy, name, number);
                }
            }
            else
            {
                MatlabValueSetter.setValue(proxy, name, arg);
            }
        }
        
        private static Object getReturnValue(MatlabThreadProxy proxy, String returnName, ClassInfo returnInfo,
                List<String> usedNames) throws MatlabInvocationException
        {
            Object returnValue;
            
            //Empty array, MATLAB's rough equivalent of null
            if(((boolean[]) proxy.returningEval("isempty(" + returnName + ");", 1)[0])[0])
            {
                returnValue = null;
            }
            //The variable is a Java object
            else if(((boolean[]) proxy.returningEval("isjava(" + returnName + ");", 1)[0])[0])
            {
                returnValue = MatlabValueReceiver.receiveValue(proxy, usedNames, returnName);
            }
            else
            {
                String type = (String) proxy.returningEval("class(" + returnName + ");", 1)[0];

                if(type.equals("function_handle"))
                {
                    MatlabFunctionHandleGetter getter = new MatlabFunctionHandleGetter();
                    getter.getInMatlab(proxy, returnName);
                    returnValue = getter;
                }
                else if(MATLAB_TO_JAVA_CLASS.containsKey(type))
                {   
                    //If a singular value
                    boolean isScalar = ((boolean[]) proxy.returningEval("isscalar(" + returnName + ");", 1)[0])[0];

                    //Whether the value should be returned as a linear array instead of MATLAB's default of the minimum
                    //array dimension being 2
                    boolean keepLinear = false;
                    if(!isScalar)
                    {
                        //returnLinearArray will be true if the array is a vector and the specified return type
                        //is the appropriate corresponding one dimensional array
                        boolean isVector = ((boolean[]) proxy.returningEval("isvector(" + returnName + ");", 1)[0])[0];
                        keepLinear = (isVector && returnInfo.arrayDimensions == 1 &&
                                     MATLAB_TO_JAVA_CLASS.get(type).equals(returnInfo.baseComponentType));
                    }
                    
                    //logical -> boolean
                    if(type.equals("logical"))
                    {
                        if(isScalar)
                        {
                            returnValue = MatlabValueReceiver.receiveValue(proxy, usedNames, returnName);
                        }
                        else
                        {   
                            PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                            getter.getInMatlab(proxy, returnName);
                            returnValue = getter;
                        }
                    }
                    //char -> char or String
                    else if(type.equals("char"))
                    {
                        //If the return type is specified as char, Character, or an array of char
                        if(char.class.equals(returnInfo.theClass) || Character.class.equals(returnInfo.theClass) ||
                           char.class.equals(returnInfo.baseComponentType))
                        {
                            if(isScalar)
                            {
                                returnValue = MatlabValueReceiver.receiveValue(proxy, usedNames, returnName);
                            }
                            else
                            {   
                                PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                                getter.getInMatlab(proxy, returnName);
                                returnValue = getter;
                            }
                        }
                        //By default retrieve it as a String or an array of Strings
                        else
                        {
                            returnValue = MatlabValueReceiver.receiveValue(proxy, usedNames, returnName);
                        }
                    }
                    //Numerics
                    //int8 -> byte, int16 -> short, int32 -> int, int64 -> long, single -> float, double -> double
                    else
                    {
                        boolean isReal = ((boolean[]) proxy.returningEval("isreal(" + returnName + ");", 1)[0])[0];
                        
                        if(isScalar)
                        {
                            if(isReal)
                            {
                                returnValue = MatlabValueReceiver.receiveValue(proxy, usedNames, returnName);
                            }
                            else
                            {
                                ComplexNumberGetter getter = new ComplexNumberGetter();
                                getter.getInMatlab(proxy, returnName);
                                returnValue = getter;
                            }
                        }
                        else
                        {
                            if(isReal)
                            {
                                PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                                getter.getInMatlab(proxy, returnName);
                                returnValue = getter;
                            }
                            else
                            {
                                MatlabNumericArrayGetter getter = new MatlabNumericArrayGetter();
                                getter.getInMatlab(proxy, returnName);
                                returnValue = getter;
                            }
                        }
                    }
                }
                else
                {
                    throw new UnsupportedOperationException("Unsupported MATLAB type: " + type);
                }
            }
            
            return returnValue;
        }
        
        private static final Map<String, Class> MATLAB_TO_JAVA_CLASS = new HashMap<String, Class>();
        static
        {
            MATLAB_TO_JAVA_CLASS.put("int8", byte.class);
            MATLAB_TO_JAVA_CLASS.put("int16", short.class);
            MATLAB_TO_JAVA_CLASS.put("int32", int.class);
            MATLAB_TO_JAVA_CLASS.put("int64", long.class);
            MATLAB_TO_JAVA_CLASS.put("single", float.class);
            MATLAB_TO_JAVA_CLASS.put("double", double.class);
            MATLAB_TO_JAVA_CLASS.put("logical", boolean.class);
            MATLAB_TO_JAVA_CLASS.put("char", char.class);
        }
        
        private static class MatlabValueSetter
        {
            private static void setValue(MatlabThreadProxy proxy, String variableName, Object value)
                    throws MatlabInvocationException
            {
                MatlabValueSetter setter = new MatlabValueSetter(value);
                proxy.setVariable(variableName, setter);
                proxy.eval(variableName + " = " + variableName + ".getValue();");
            }
            
            private final Object _value;
            
            private MatlabValueSetter(Object value)
            {
                _value = value;
            }
            
            public Object getValue()
            {
                return _value;
            }
        }
        
        private static class MatlabValueReceiver
        {
            private static Object receiveValue(MatlabThreadProxy proxy, List<String> usedNames,
                    String variableName) throws MatlabInvocationException
            {
                String receiverName = (String) proxy.returningEval("genvarname('receiver_', who);", 1)[0];
                MatlabValueReceiver receiver = new MatlabValueReceiver();
                proxy.setVariable(receiverName, receiver);
                usedNames.add(receiverName);
                proxy.eval(receiverName + ".set(" + variableName + ");");
                
                return receiver._value;
            }
            
            private Object _value = null;
            
            public void set(Object val)
            {
                _value = val;
            }
        }
        
        private List<String> generateNames(MatlabThreadProxy proxy, String root, int amount)
                throws MatlabInvocationException
        {
            //Build set of currently taken names
            Set<String> takenNames = new HashSet<String>(Arrays.asList((String[]) proxy.returningEval("who", 1)[0]));
            
            //Generate names
            List<String> generatedNames = new ArrayList<String>();
            int genSequenence = 0;
            while(generatedNames.size() != amount)
            {
                String generatedName = root + genSequenence;
                while(takenNames.contains(generatedName))
                {
                    genSequenence++;
                    generatedName = root + genSequenence;
                }
                genSequenence++;
                generatedNames.add(generatedName);
            }
            
            return generatedNames;
        }
    }
}