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
import matlabcontrol.extensions.MatlabReturns.ReturnN;
import matlabcontrol.extensions.MatlabType.MatlabTypeSerializedGetter;

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
            
            //Build information about how to invoke the method, performing validation in the process
            FunctionInfo functionInfo = getFunctionInfo(method, annotation);
            Class<?>[] returnTypes = getReturnTypes(method);
            Class<?>[] parameterTypes = method.getParameterTypes();
            InvocationInfo invocationInfo = new InvocationInfo(functionInfo.name, functionInfo.containingDirectory,
                    returnTypes, parameterTypes);
            resolvedInfo.put(method, invocationInfo);
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
        
        //Check that all of the return types are supported
        for(Class returnType : returnTypes)
        {
            ClassInfo info = ClassInfo.getInfo(returnType);
            
            //char arrays as a return type are only supported up to 9 dimensions
            if(char.class.equals(info.arrayElementClass) &&
               info.arrayDimensions > CustomFunctionInvocation.MatlabCharReceiver.MAX_DIMENSION_SUPPORTED)
            {
                throw new LinkingException(method + " may not have a return type of a char array in excess of " +
                        CustomFunctionInvocation.MatlabCharReceiver.MAX_DIMENSION_SUPPORTED + " dimensions\n" +
                        "Return Type: " + returnType.getCanonicalName() + "\n" +
                        "Dimensions: " + info.arrayDimensions);
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
        
        return getMultidimensionalArrayClass((Class<?>) componentType, dimensions, method);
    }
    
    private static final Map<Class<?>, String> PRIMITIVE_TO_BINARY_NAME = new ConcurrentHashMap<Class<?>, String>();
    static
    {
        PRIMITIVE_TO_BINARY_NAME.put(byte.class, "B");
        PRIMITIVE_TO_BINARY_NAME.put(short.class, "S");
        PRIMITIVE_TO_BINARY_NAME.put(int.class, "I");
        PRIMITIVE_TO_BINARY_NAME.put(long.class, "J");
        PRIMITIVE_TO_BINARY_NAME.put(float.class, "F");
        PRIMITIVE_TO_BINARY_NAME.put(double.class, "D");
        PRIMITIVE_TO_BINARY_NAME.put(boolean.class, "Z");
        PRIMITIVE_TO_BINARY_NAME.put(char.class, "C");
    }
    
    /**
     * 
     * @param componentType
     * @param dimensions
     * @param method used for exception message only
     * @return 
     */
    private static Class<?> getMultidimensionalArrayClass(Class<?> componentType, int dimensions, Method method)
    {
        String prefix = "";
        String suffix = "";
        String body;
        
        for(int i = 0; i < dimensions; i++)
        {
            prefix += "[";
        }
        
        if(componentType.isPrimitive())
        {
            body = PRIMITIVE_TO_BINARY_NAME.get(componentType);
        }
        else
        {
            prefix += "L";
            suffix = ";";
            body = componentType.getName();
        }
        
        String binaryName = prefix + body + suffix;
        
        try
        {
            return Class.forName(binaryName, false, componentType.getClassLoader());
        }
        catch(ClassNotFoundException e)
        {
            throw new LinkingException(method + " has a " + dimensions + " dimension array of " +
                    componentType.getCanonicalName() + " as a parameter of " +
                    method.getReturnType().getCanonicalName() + " which could be not be created with the binary name " +
                    binaryName, e);
        }
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
        
        /**
         * The declared types of the arguments of the method associated with the function.
         */
        final Class<?>[] parameterTypes;
        
        private InvocationInfo(String name, String containingDirectory,
                Class<?>[] returnTypes, Class<?>[] parameterTypes)
        {
            this.name = name;
            this.containingDirectory = containingDirectory;
            
            this.returnTypes = returnTypes;
            this.parameterTypes = parameterTypes;
        }
        
        @Override
        public String toString()
        {
            return "[" + this.getClass().getName() + 
                    " name=" + name + "," +
                    " containingDirectory=" + containingDirectory + "," +
                    " returnTypes=" + Arrays.asList(returnTypes) + "," +
                    " parameterTypes=" + Arrays.asList(parameterTypes) + "]";
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
        
        final boolean isVoid;
        final boolean isPrimitive;
        final Class<?> autoboxedClass; //Autoboxed equivalent of the class, if the class is primitive
        
        final boolean isAutoboxedPrimitive;
        final Class<?> primitiveClass; //Primitive equivalent of the class, if the class is autoboxed
        
        final boolean isArray;
        final boolean isPrimitiveArray;
        final Class<?> arrayElementClass; //The type of the array, if the class is an array
        final int arrayDimensions;
        
        final boolean isMatlabType;
        
        private ClassInfo(Class<?> clazz)
        {
            isPrimitive = clazz.isPrimitive();
            autoboxedClass = PRIMITIVE_TO_AUTOBOXED.get(clazz);
            
            isAutoboxedPrimitive = AUTOBOXED_TO_PRIMITIVE.containsKey(clazz);
            primitiveClass = AUTOBOXED_TO_PRIMITIVE.get(clazz);
            
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
                arrayElementClass = type;
                isPrimitiveArray = type.isPrimitive();
            }
            else
            {
                isArray = false;
                arrayElementClass = null;
                isPrimitiveArray = false;
                arrayDimensions = 0;
            }
            
            isVoid = clazz.equals(Void.class) || clazz.equals(void.class);
            isMatlabType = MatlabType.class.isAssignableFrom(clazz);
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
        
        private static final Map<Class<?>, Class<?>> AUTOBOXED_TO_PRIMITIVE = new ConcurrentHashMap<Class<?>, Class<?>>();
        static
        {
            AUTOBOXED_TO_PRIMITIVE.put(Byte.class, byte.class);
            AUTOBOXED_TO_PRIMITIVE.put(Short.class, short.class);
            AUTOBOXED_TO_PRIMITIVE.put(Integer.class, int.class);
            AUTOBOXED_TO_PRIMITIVE.put(Long.class, long.class);
            AUTOBOXED_TO_PRIMITIVE.put(Double.class, double.class);
            AUTOBOXED_TO_PRIMITIVE.put(Float.class, float.class);
            AUTOBOXED_TO_PRIMITIVE.put(Boolean.class, boolean.class);
            AUTOBOXED_TO_PRIMITIVE.put(Character.class, char.class);
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
            
            //Replace all arguments with parameters of a MatlabType subclass or a multidimensional primitive array with
            //their serialized setters
            for(int i = 0; i < args.length; i++)
            {
                if(args[i] == null)
                {
                    continue;
                }
                
                ClassInfo paramInfo = ClassInfo.getInfo(info.parameterTypes[i]);

                if(paramInfo.isMatlabType)
                {
                    args[i] = ((MatlabType) args[i]).getSerializedSetter();
                }
                else if(paramInfo.isPrimitiveArray && paramInfo.arrayDimensions > 1)
                {
                    args[i] = ArrayLinearizer.getSerializedSetter(args[i]);
                }
            }

            //Invoke function
            Object[] returnValues = _proxy.invokeAndWait(new CustomFunctionInvocation(info, args));

            //For each returned value that is a serialized getter, deserialize it
            for(int i = 0; i < returnValues.length; i++)
            {
                if(returnValues[i] instanceof MatlabTypeSerializedGetter)
                {
                    returnValues[i] = ((MatlabType.MatlabTypeSerializedGetter) returnValues[i]).deserialize();
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
                toReturn = convertToType(result[0], returnTypes[0]);
            }
            //Return type is a subclass of ReturnN
            else
            {
                for(int i = 0; i < result.length; i++)
                {
                    result[i] = convertToType(result[i], returnTypes[i]);
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
        
        private Object convertToType(Object value, Class<?> returnType)
        {
            ClassInfo returnInfo = ClassInfo.getInfo(returnType);
            
            Object toReturn;
            if(value == null)
            {
                toReturn = null;
            }
            else if(returnInfo.isPrimitive)
            {
                toReturn = convertPrimitiveReturnType(value, returnType);
            }
            else if(returnInfo.isAutoboxedPrimitive)
            {
                toReturn = convertAutoboxedReturnType(value, returnType);
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
                                "have exactly 1 value");
                }
                
                result = Array.get(value, 0);
            }
            else
            {
                throw new IncompatibleReturnException("Required return type is incompatible with the type actually " +
                        "returned\n" +
                        "Required type: " + returnType.getCanonicalName() + "\n" +
                        "Returned type: " + actualType.getCanonicalName());
            }
            
            return result;
        }
        
        private static final Map<Class<?>, Class<?>> AUTOBOXED_TO_PRIMITIVE_ARRAY = new ConcurrentHashMap<Class<?>, Class<?>>();
        static
        {
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Byte.class, byte[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Short.class, short[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Integer.class, int[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Long.class, long[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Double.class, double[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Float.class, float[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Boolean.class, boolean[].class);
            AUTOBOXED_TO_PRIMITIVE_ARRAY.put(Character.class, char[].class);
        }
        
        private Object convertAutoboxedReturnType(Object value, Class<?> returnType)
        {
            Class<?> actualType = value.getClass();
            
            Class<?> primitiveArrayOfReturnType = AUTOBOXED_TO_PRIMITIVE_ARRAY.get(returnType);
            
            Object result;
            if(actualType.equals(returnType))
            {
                result = value;
            }
            else if(actualType.equals(primitiveArrayOfReturnType))
            {
                if(Array.getLength(value) != 1)
                {
                    throw new IncompatibleReturnException("Array of " + 
                            primitiveArrayOfReturnType.getComponentType().getCanonicalName() + " does not have " +
                            "exactly 1 value");
                }
                
                result = Array.get(value, 0);
            }
            else
            {
                throw new IncompatibleReturnException("Required return type is incompatible with the type actually " +
                        "returned\n" + 
                        "Required type: " + returnType.getCanonicalName() + "\n" +
                        "Returned type: " + actualType.getCanonicalName());
            }
            
            return result;
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
                String[] parameterNames = generateNames(proxy, "param_", _args.length);
                for(int i = 0; i < _args.length; i++)
                {
                    Object arg = _args[i];
                    String name = parameterNames[i];
                    usedNames.add(name);
                    
                    if(arg instanceof MatlabType.MatlabTypeSerializedSetter)
                    {
                        ((MatlabType.MatlabTypeSerializedSetter) arg).setInMatlab(proxy, name);
                    }
                    else
                    {
                        proxy.setVariable(name, arg);
                    }
                    
                    functionStr += name;
                    if(i != _args.length - 1)
                    {
                        functionStr += ", ";
                    }
                }
                functionStr += ");";
                
                //Return arguments
                String[] returnNames = new String[0];
                if(_functionInfo.returnTypes.length != 0)
                {
                    returnNames = generateNames(proxy, "return_", _functionInfo.returnTypes.length);
                    String returnStr = "[";
                    for(int i = 0; i < returnNames.length; i++)
                    {
                        String name;
                        if(_functionInfo.returnTypes[i].equals(Void.class))
                        {
                            name = "~";
                        }
                        else
                        {
                            name = returnNames[i];
                            usedNames.add(name);
                        }
                        
                        returnStr += name;
                        if(i != returnNames.length - 1)
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
                    Class<?> returnType = _functionInfo.returnTypes[i];
                    ClassInfo returnInfo = ClassInfo.getInfo(returnType);
                    String returnName = returnNames[i];
                    
                    if(returnInfo.isVoid)
                    {
                        returnValues[i] = null;
                    }
                    else if(returnInfo.isMatlabType)
                    {
                        MatlabTypeSerializedGetter getter =
                                MatlabType.newSerializedGetter((Class<? extends MatlabType>) returnType);
                        getter.getInMatlab(proxy, returnName);
                        returnValues[i] = getter;
                    }
                    else
                    {
                        String receiverName = generateNames(proxy, "receiver_", 1)[0];
                        usedNames.add(receiverName);
                        
                        //If a char, Character, or char array handle it specially so that MATLAB does not convert it to
                        //a String or String array
                        if(returnType.equals(char.class) || returnType.equals(Character.class) || 
                           char.class.equals(returnInfo.arrayElementClass))
                        {
                            proxy.setVariable(receiverName, new MatlabCharReceiver(returnValues, i));
                        }
                        else
                        {
                            proxy.setVariable(receiverName, new MatlabValueReceiver(returnValues, i));
                        }
                        proxy.eval(receiverName + ".set(" + returnName + ");");
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
                        String variablesStr = "";
                        for(int i = 0; i < usedNames.size(); i++)
                        {
                            variablesStr += usedNames.get(i);

                            if(i != usedNames.size() - 1)
                            {
                                variablesStr += " ";
                            }
                        }
                        proxy.eval("clear " + variablesStr);
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
        
        private static class MatlabValueReceiver
        {
            private final Object[] _values;
            private final int _index;
            
            private MatlabValueReceiver(Object[] values, int index)
            {
                _values = values;
                _index = index;
            }
            
            public void set(Object val)
            {
                _values[_index] = val;
            }
        }
        
        private static class MatlabCharReceiver
        {
            static final int MAX_DIMENSION_SUPPORTED = 9;
                    
            private final Object[] _values;
            private final int _index;
            
            private MatlabCharReceiver(Object[] values, int index)
            {
                _values = values;
                _index = index;
            }
            
            public void set(char val)
            {
                _values[_index] = val;
            }
            
            public void set(char[] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][][][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][][][][][] val)
            {
                _values[_index] = val;
            }
            
            public void set(char[][][][][][][][][] val)
            {
                _values[_index] = val;
            }
            
            //If this is called then val's type is almost certainly not going to match the return type
            public void set(Object val)
            {
                _values[_index] = val;
            }
        }
        
        private String[] generateNames(MatlabThreadProxy proxy, String root, int amount)
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
            
            return generatedNames.toArray(new String[generatedNames.size()]);
        }
    }
}