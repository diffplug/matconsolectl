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
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
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
import matlabcontrol.MatlabOperations;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.link.ArrayMultidimensionalizer.PrimitiveArrayGetter;
import matlabcontrol.link.MatlabNumber.MatlabNumberGetter;
import matlabcontrol.link.MatlabFunctionHandle.MatlabFunctionHandleGetter;
import matlabcontrol.link.MatlabNumberArray.MatlabNumberArrayGetter;
import matlabcontrol.link.MatlabReturns.ReturnN;
import matlabcontrol.link.MatlabType.MatlabTypeGetter;
import matlabcontrol.link.MatlabType.MatlabTypeSetter;
import matlabcontrol.link.MatlabVariable.MatlabVariableGetter;

/**
 *
 * @since 5.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class Linker
{   
    private Linker() { }
    
    
    /**************************************************************************************************************\
    |*                                        Linking & Validation                                                *|
    \**************************************************************************************************************/
    
    public MatlabOperations getLinkedMatlabOperations(MatlabProxy proxy)
    {   
        if(proxy == null)
        {
            throw new NullPointerException("proxy may not be null");
        }
        
        Class<?> opsClass = MatlabOperations.class;
        MatlabOperations operations = (MatlabOperations) Proxy.newProxyInstance(opsClass.getClassLoader(),
                new Class<?>[] { opsClass },
                new MatlabFunctionInvocationHandler(proxy, opsClass, new ConcurrentHashMap<Method, InvocationInfo>()));
        
        return operations;
    }
    
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
            MatlabFunction annotation = method.getAnnotation(MatlabFunction.class);
            if(annotation == null)
            {
                throw new LinkingException(method + " is not annotated with " +
                        MatlabFunction.class.getCanonicalName());
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
    
    private static FunctionInfo getFunctionInfo(Method method, MatlabFunction annotation)
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
            
            //Retrieve location of function file
            File functionFile;
            if(new File(path).isAbsolute())
            {
                functionFile = new File(path);
            }
            else
            {
                functionFile = resolveRelativePath(method, path);
            }
            
            //Resolve canonical path
            try
            {
                functionFile = functionFile.getCanonicalFile();
            }
            catch(IOException e)
            {
                throw new LinkingException("Unable to resolve canonical path of specified function\n" +
                        "method: " + method.getName() + "\n" +
                        "path:" + path + "\n" +
                        "non-canonical path: " + functionFile.getAbsolutePath(), e);
            }
            
            //Validate file location
            if(!functionFile.exists())
            {
                throw new LinkingException("Specified file does not exist\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + functionFile.getAbsolutePath());
            }
            if(!functionFile.isFile())
            {
                throw new LinkingException("Specified file is not a file\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + functionFile.getAbsolutePath());
            }
            if(!(functionFile.getName().endsWith(".m") || functionFile.getName().endsWith(".p")))
            {
                throw new LinkingException("Specified file does not end in .m or .p\n" + 
                        "method: " + method.getName() + "\n" +
                        "path: " + path + "\n" +
                        "resolved as: " + functionFile.getAbsolutePath());
            }
            
            //Parse out the name of the function and the directory containing it
            containingDirectory = functionFile.getParent();
            functionName = functionFile.getName().substring(0, functionFile.getName().length() - 2);
            
            //Validate the function name
            if(!isFunctionName(functionName))
            {
                throw new LinkingException("Specified file's name is not a MATLAB function name\n" + 
                        "Function Name: " + functionName + "\n" +
                        "File: " + functionFile.getAbsolutePath());
            }
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
     * Cache used by {@link #resolveRelativePath(Method, String)}
     */
    private static final ConcurrentHashMap<Class<?>, Map<String, File>> UNZIPPED_ENTRIES =
            new ConcurrentHashMap<Class<?>, Map<String, File>>();
    
    /**
     * Resolves the location of {@code relativePath} relative to the interface which declared {@code method}. If the
     * interface is inside of a zip file (jar/war/ear etc. file) then the contents of the zip file may first need to be
     * unzipped.
     * 
     * @param method
     * @param relativePath
     * @return the absolute location of the file
     */
    private static File resolveRelativePath(Method method, String relativePath)
    {   
        Class<?> theInterface = method.getDeclaringClass();
        File interfaceLocation = getClassLocation(theInterface); 
        
        File functionFile;
        
        //Code source is in a file, which means it should be jar/ear/war/zip etc.
        if(interfaceLocation.isFile())
        {        
            if(!UNZIPPED_ENTRIES.containsKey(theInterface))
            {
                UnzipResult unzipResult = unzip(interfaceLocation);
                Map<String, File> mapping = unzipResult.unzippedMapping;
                List<File> filesToDelete = new ArrayList<File>(mapping.values());
                filesToDelete.add(unzipResult.rootDirectory); 
                
                //If there was a previous mapping, delete all of the files just unzipped 
                if(UNZIPPED_ENTRIES.putIfAbsent(theInterface, mapping) != null)
                {
                    deleteFiles(filesToDelete, true);
                }
                //No previous mapping, delete unzipped files on JVM exit
                else
                {
                    deleteFiles(filesToDelete, false);
                }
            }
            
            functionFile = UNZIPPED_ENTRIES.get(theInterface).get(relativePath);
            
            if(functionFile == null)
            {
                 throw new LinkingException("Unable to find file inside of zip\n" +
                    "Method: " + method.getName() + "\n" +
                    "Relative Path: " + relativePath + "\n" +
                    "Zip File: " + interfaceLocation.getAbsolutePath());
            }
        }
        //Code source is a directory, it code should not be inside a jar/ear/war/zip etc.
        else
        {
            functionFile = new File(interfaceLocation, relativePath);
        }
        
        return functionFile;
    }
    
    private static void deleteFiles(Collection<File> files, boolean deleteNow)
    {
        ArrayList<File> sortedFiles = new ArrayList<File>(files);
        Collections.sort(sortedFiles);
        
        //Delete files in the opposite order so that files are deleted before their containing directories
        if(deleteNow)
        {
            for(int i = sortedFiles.size() - 1; i >= 0; i--)
            {
                sortedFiles.get(i).delete();
            }
        }
        //Delete files in the existing order because the files will be deleted on exit in the opposite order
        else
        {
            for(File file : sortedFiles)
            {
                file.deleteOnExit();
            }
        }
    }
    
    /**
     * Cache used by {@link #getClassLocation(java.lang.Class)}}.
     */
    private static final ConcurrentHashMap<Class<?>, File> CLASS_LOCATIONS =
            new ConcurrentHashMap<Class<?>, File>();
    
    private static File getClassLocation(Class<?> clazz)
    {
        if(!CLASS_LOCATIONS.containsKey(clazz))
        {
            try
            {
                URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
                File file = new File(url.toURI().getPath()).getCanonicalFile();
                if(!file.exists())
                {
                    throw new LinkingException("Incorrectly resolved location of class\n" +
                            "class: " + clazz.getCanonicalName() + "\n" +
                            "focation: " + file.getAbsolutePath());
                }

                CLASS_LOCATIONS.put(clazz, file);
            }
            catch(IOException e)
            {
                throw new LinkingException("Unable to determine location of " + clazz.getCanonicalName(), e);
            }
            catch(URISyntaxException e)
            {
                throw new LinkingException("Unable to determine location of " + clazz.getCanonicalName(), e);
            }
        }
        
        return CLASS_LOCATIONS.get(clazz);
    }
    
    private static class UnzipResult
    {
        final Map<String, File> unzippedMapping;
        File rootDirectory;
        
        private UnzipResult(Map<String, File> mapping, File root)
        {
            this.unzippedMapping = mapping;
            this.rootDirectory = root;
        }
    }
    
    /**
     * Unzips the file located at {@code zipLocation}.
     * 
     * @param zipLocation the location of the file zip
     * @return resulting files from unzipping
     * @throws LinkingException if unable to unzip the zip file for any reason
     */
    private static UnzipResult unzip(File zipLocation)
    {
        ZipFile zip;
        try
        {
            zip = new ZipFile(zipLocation);
        }
        catch(IOException e)
        {
            throw new LinkingException("Unable to open zip file\n" +
                    "zip location: " + zipLocation.getAbsolutePath(), e);
        }
        
        try
        {
            //Mapping from entry names to the unarchived location on disk
            Map<String, File> entryMap = new HashMap<String, File>();
            
            //Destination
            File unzipDir = new File(System.getProperty("java.io.tmpdir"), "linked_" + UUID.randomUUID().toString());

            for(Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); )
            {
                ZipEntry entry = entries.nextElement();

                //Directory
                if(entry.isDirectory())
                {
                    File destDir = new File(unzipDir, entry.getName());
                    destDir.mkdirs();
                    
                    entryMap.put(entry.getName(), destDir);
                }
                //File
                else
                {
                    //File should not exist, but confirm it
                    File destFile = new File(unzipDir, entry.getName());
                    if(destFile.exists())
                    {
                        throw new LinkingException("Cannot unzip file, randomly generated path already exists\n" +
                                "generated path: " + destFile.getAbsolutePath() + "\n" +
                                "zip file: " + zipLocation.getAbsolutePath());
                    }
                    destFile.getParentFile().mkdirs();

                    //Unarchive
                    try
                    {
                        final int BUFFER_SIZE = 2048;
                        OutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
                        try
                        {
                            InputStream entryStream = zip.getInputStream(entry);
                            try
                            {
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int count;
                                while((count = entryStream.read(buffer, 0, BUFFER_SIZE)) != -1)
                                {
                                   dest.write(buffer, 0, count);
                                }
                                dest.flush();
                            }
                            finally
                            {
                                entryStream.close();
                            }
                        }
                        finally
                        {
                            dest.close();
                        }
                    }
                    catch(IOException e)
                    {
                        throw new LinkingException("Unable to unzip file entry\n" +
                                "entry: " + entry.getName() + "\n" +
                                "zip location: " + zipLocation.getAbsolutePath() + "\n" +
                                "destination file: " + destFile.getAbsolutePath(), e);
                    }
                
                    entryMap.put(entry.getName(), destFile);
                }
            }
            
            return new UnzipResult(Collections.unmodifiableMap(entryMap), unzipDir);
        }
        finally
        {
            try
            {
                zip.close();
            }
            catch(IOException ex)
            {
                throw new LinkingException("Unable to close zip file: " + zipLocation.getAbsolutePath());
            }
        }
    }
    
    private static class ReturnTypeInfo
    {
        Class<?>[] returnTypes;
        Class<?>[][] returnTypeParameters;
        
        private ReturnTypeInfo(Class<?>[] returnTypes, Class<?>[][] returnTypeParameters)
        {
            this.returnTypes = returnTypes;
            this.returnTypeParameters = returnTypeParameters;
        }
    }
    
    private static ReturnTypeInfo getReturnTypes(Method method)
    {
        //The type-erasured return type of the method
        Class<?> methodReturn = method.getReturnType();
        
        //The return type of the method with type information
        Type genericReturn = method.getGenericReturnType();
        
        //The return types to be determined
        Class<?>[] returnTypes;
        Class<?>[][] returnTypeGenericParameters;
        
        //0 return arguments
        if(methodReturn.equals(void.class))
        {
            returnTypes = new Class<?>[0];
            returnTypeGenericParameters = new Class<?>[0][0];
        }
        //1 return argument
        else if(!ReturnN.class.isAssignableFrom(methodReturn))
        {
            //MatlabNumberArray subclasses are allowed to be parameterized
            if(MatlabNumberArray.class.isAssignableFrom(methodReturn))
            {
                if(genericReturn instanceof ParameterizedType)
                {
                    Type[] parameterizedTypes = ((ParameterizedType) genericReturn).getActualTypeArguments();
                    Class<?> parameter = getMatlabNumberArrayParameter(parameterizedTypes[0], methodReturn, method);
                    returnTypeGenericParameters = new Class<?>[][] { { parameter } };
                }
                else
                {
                    throw new LinkingException(method + " must parameterize " + methodReturn.getCanonicalName());
                }
            }
            else if(!methodReturn.equals(genericReturn))
            {
                throw new LinkingException(method + " may not have a return type that uses generics");
            }
            else
            {
                returnTypeGenericParameters = new Class<?>[1][0];
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
                returnTypeGenericParameters = new Class<?>[parameterizedTypes.length][];
                
                for(int i = 0; i < parameterizedTypes.length; i++)
                {
                    Type type = parameterizedTypes[i];
                
                    if(type instanceof Class)
                    {
                        Class returnType = (Class) type;
                        if(MatlabNumberArray.class.isAssignableFrom(returnType))
                        {
                            throw new LinkingException(method + " must parameterize " + returnType.getCanonicalName());
                        }
                        returnTypes[i] = returnType;
                        returnTypeGenericParameters[i] = new Class[0];
                    }
                    else if(type instanceof GenericArrayType)
                    {   
                        returnTypes[i] = getClassOfArrayType((GenericArrayType) type, method);
                        returnTypeGenericParameters[i] = new Class[0];
                    }
                    else if(type instanceof ParameterizedType)
                    {
                        //MatlabNumberArray subclasses are allowed to be parameterized
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        Type rawType = parameterizedType.getRawType();
                        if(rawType instanceof Class && MatlabNumberArray.class.isAssignableFrom((Class<?>) rawType))
                        {
                            Class<?> returnType = (Class<?>) rawType;
                            returnTypes[i] = returnType;
                            
                            Class<?> parameter = getMatlabNumberArrayParameter(
                                    parameterizedType.getActualTypeArguments()[0], returnType, method);
                            returnTypeGenericParameters[i] = new Class[] { parameter };
                        }
                        else
                        {
                            throw new LinkingException(method + " may not parameterize " +
                                    methodReturn.getCanonicalName() + " with a parameterized type");
                        }
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
        
        return new ReturnTypeInfo(returnTypes, returnTypeGenericParameters);
    }
    
    private static Class<?> getMatlabNumberArrayParameter(Type type, Class<?> matlabArrayClass, Method method)
    {
        Class<?> parameter;
        if(type instanceof GenericArrayType)
        {
            parameter = getClassOfArrayType((GenericArrayType) type, method);
            ClassInfo paramInfo = ClassInfo.getInfo(parameter);
            
            boolean validParameter =
            ((matlabArrayClass.equals(MatlabInt8Array.class)   && byte.class.equals(paramInfo.baseComponentType))  ||
             (matlabArrayClass.equals(MatlabInt16Array.class)  && short.class.equals(paramInfo.baseComponentType)) || 
             (matlabArrayClass.equals(MatlabInt32Array.class)  && int.class.equals(paramInfo.baseComponentType))   ||
             (matlabArrayClass.equals(MatlabInt64Array.class)  && long.class.equals(paramInfo.baseComponentType))  ||
             (matlabArrayClass.equals(MatlabSingleArray.class) && float.class.equals(paramInfo.baseComponentType)) ||
             (matlabArrayClass.equals(MatlabDoubleArray.class) && double.class.equals(paramInfo.baseComponentType)));
            
            if(!validParameter)
            {
                throw new LinkingException(method + " may not parameterize " + matlabArrayClass.getCanonicalName() + 
                        " with " + parameter.getCanonicalName());
            }
        }
        else
        {
            throw new LinkingException(method + " may not parameterize " + matlabArrayClass.getCanonicalName() +
                    " with " + type);
        }
        
        return parameter;
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
        
        return ArrayTransformUtils.getArrayClass((Class<?>) componentType, dimensions);
    }
    
    private static InvocationInfo getInvocationInfo(Method method, MatlabFunction annotation)
    {   
        FunctionInfo funcInfo = getFunctionInfo(method, annotation);
        ReturnTypeInfo returnInfo = getReturnTypes(method);
        InvocationInfo invocationInfo = new InvocationInfo(funcInfo.name, funcInfo.containingDirectory,
                returnInfo.returnTypes, returnInfo.returnTypeParameters);
            
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
        
        /**
         * Array of generic type parameters of the return types. For instance for return type at index <i>i</i> in
         * {@code returnTypes} the generic parameters are in the array returned by {@code returnTypeGenericParameters}
         * at index <i>i</i>.
         */
        final Class<?>[][] returnTypeParameters;
        
        private InvocationInfo(String name, String containingDirectory,
                 Class<?>[] returnTypes, Class<?>[][] returnTypeParameters)
        {
            this.name = name;
            this.containingDirectory = containingDirectory;
            
            this.returnTypes = returnTypes;
            this.returnTypeParameters = returnTypeParameters;
        }
        
        @Override
        public String toString()
        {   
            String genericParameters = "[";
            for(int i = 0; i < returnTypeParameters.length; i++)
            {
                genericParameters += classArrayToString(returnTypeParameters[i]);
                
                if(i != returnTypeParameters.length - 1)
                {
                    genericParameters += " ";
                }
            }
            genericParameters += "]";
            
            return "[" + this.getClass().getSimpleName() + 
                    " name=" + name + "," +
                    " containingDirectory=" + containingDirectory + "," +
                    " returnTypes=" + classArrayToString(returnTypes) + "," +
                    " returnTypesGenericParameters=" + genericParameters + "]";
        }
        
        private static String classArrayToString(Class[] array)
        {
            String str = "[";
            for(int i = 0; i < array.length; i++)
            {
                str += array[i].getCanonicalName();
                
                if(i != array.length - 1)
                {
                    str += " ";
                }
            }
            str += "]";
            
            return str;
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
        final Class<?> describedClass;
        
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
         * If the class is one of: {@code byte}, {@code Byte}, {@code short}, {@code Short}, {@code int},
         * {@code Integer}, {@code long}, {@code Long}, {@code float}, {@code Float}, {@code double}, {@code Double}
         */
        final boolean isBuiltinNumeric;
        
        /**
         * If the class inherits from {@code MatlabType}
         */
        final boolean isMatlabType;
        
        private ClassInfo(Class<?> clazz)
        {
            describedClass = clazz;
            
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
            
            isBuiltinNumeric = clazz.equals(Byte.class)    || clazz.equals(byte.class)  ||
                               clazz.equals(Short.class)   || clazz.equals(short.class) || 
                               clazz.equals(Integer.class) || clazz.equals(int.class)   ||    
                               clazz.equals(Long.class)    || clazz.equals(long.class)  ||
                               clazz.equals(Float.class)   || clazz.equals(float.class) || 
                               clazz.equals(Double.class)  || clazz.equals(double.class);
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
                ConcurrentMap<Method, InvocationInfo> invocationInfos)
        {
            _proxy = proxy;
            _interface = functionInterface;
            _invocationsInfo = invocationInfos;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws MatlabInvocationException
        {   
            Object result;
            
            //Method belongs to java.lang.Object
            if(method.getDeclaringClass().equals(Object.class))
            {
                result = invokeObjectMethod(method, args);
            }
            //Methods defined in matlabcontrol.MatlabOperations interface
            else if(method.getDeclaringClass().equals(MatlabOperations.class))
            {
                result = invokeMatlabOperationsMethod(method, args);
            }
            //Method belongs to the interface supplied to this linker
            else
            {
                result = invokeUserInterfaceMethod(method, args);
            }
            
            return result;
        }
        
        private Object invokeObjectMethod(Method method, Object[] args)
        {
            Object result;
            
            //public void String toString()
            if(method.getName().equals("toString"))
            {
                result = "[Linked " + _interface.getCanonicalName() + " info=" + _invocationsInfo + "]";
            }
            //public boolean equals(Object other)
            else if(method.getName().equals("equals"))
            {
                Object other = args[0];
                if(other != null && Proxy.isProxyClass(other.getClass()) &&
                   (Proxy.getInvocationHandler(other) instanceof MatlabFunctionInvocationHandler))
                {
                    InvocationHandler handler = Proxy.getInvocationHandler(other);
                    result = _interface.equals(((MatlabFunctionInvocationHandler) handler)._interface);
                }
                else
                {
                    result = false;
                }
            }
            //public int hashCode()
            else if(method.getName().equals("hashCode"))
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
        
        private Object invokeMatlabOperationsMethod(Method method, Object[] args) throws MatlabInvocationException
        {
            Class<?> methodReturn = method.getReturnType();
            Object result;
            
            if(method.getName().equals("eval"))
            {
                InvocationInfo info = new InvocationInfo("eval", null, new Class<?>[0], new Class<?>[0][0]);
                
                result = invokeMatlabFunction(info, false, args, methodReturn);
            }
            else if(method.getName().equals("returningEval"))
            {   
                //Each return type
                int nargout = (Integer) args[1];
                Class<?>[] returnTypes = new Class<?>[nargout];
                Arrays.fill(returnTypes, Object.class);
                
                InvocationInfo info = new InvocationInfo("eval", null, returnTypes, new Class<?>[nargout][0]);
                
                result = invokeMatlabFunction(info, false, new Object[]{ args[0] }, methodReturn);
            }
            else if(method.getName().equals("feval"))
            {
                //Make function arguments at the same level as the function name
                Object[] functionArgs = (Object[]) args[1];
                Object[] firstLevelArgs = new Object[functionArgs.length + 1];
                firstLevelArgs[0] = args[0];
                System.arraycopy(functionArgs, 0, firstLevelArgs, 1, functionArgs.length);
                
                InvocationInfo info = new InvocationInfo("feval", null, new Class<?>[0], new Class<?>[0][0]);
                
                result = invokeMatlabFunction(info, false, firstLevelArgs, methodReturn);
            }
            else if(method.getName().equals("returningFeval"))
            {
                //Make function arguments at same level as function name
                Object[] functionArgs = (Object[]) args[2];
                Object[] firstLevelArgs = new Object[functionArgs.length + 1];
                firstLevelArgs[0] = args[0];
                System.arraycopy(functionArgs, 0, firstLevelArgs, 1, functionArgs.length);
                
                //Each return type
                int nargout = (Integer) args[1];
                Class<?>[] returnTypes = new Class<?>[nargout];
                Arrays.fill(returnTypes, Object.class);
                
                InvocationInfo info = new InvocationInfo("feval", null, returnTypes, new Class<?>[nargout][0]);
                
                result = invokeMatlabFunction(info, false, firstLevelArgs, methodReturn);
            }
            else if(method.getName().equals("getVariable"))
            {
                InvocationInfo info = new InvocationInfo("eval", null, new Class<?>[]{ Object.class }, new Class<?>[1][0]);
                
                result = ((Object[]) invokeMatlabFunction(info, false, args, methodReturn))[0];
            }
            else if(method.getName().equals("setVariable"))
            {
                InvocationInfo info = new InvocationInfo("assignin", null, new Class<?>[0], new Class<?>[0][0]);
                
                result = invokeMatlabFunction(info, false, new Object[]{ "base", args[0], args[1] }, methodReturn);
            }
            else
            {
                throw new UnsupportedOperationException(method + " not supported");
            }
            
            return result;
        }
        
        private Object invokeUserInterfaceMethod(Method method, Object[] args) throws MatlabInvocationException
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
            
            return invokeMatlabFunction(info, true, args, method.getReturnType());
        }
        
        private Object invokeMatlabFunction(InvocationInfo info, boolean userDefined, Object[] args,
                Class<?> methodReturn)
                throws MatlabInvocationException
        {
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
                        args[i] = ArrayLinearizer.getSetter(args[i]);
                    }
                }
            }

            //Invoke function
            FunctionResult result = _proxy.invokeAndWait(new CustomFunctionInvocation(info, args));
            if(result.thrownException != null)
            {
                result.thrownException.fillInStackTrace();
                throw result.thrownException;
            }
            Object[] returnValues = result.returnArgs;

            //For each returned value that is a serialized getter, retrieve it
            for(int i = 0; i < returnValues.length; i++)
            {
                if(returnValues[i] instanceof MatlabTypeGetter)
                {
                    returnValues[i] = ((MatlabType.MatlabTypeGetter) returnValues[i]).retrieve();
                }
            }
            
            //Process returned values if user defined
            Object toReturn;
            if(userDefined)
            {
                toReturn = processReturnValues(info, returnValues, methodReturn);
            }
            else
            {
                toReturn = returnValues;
            }
            
            return toReturn; 
        }
        
        private Object processReturnValues(InvocationInfo info, Object[] result, Class<?> methodReturn)
        {
            Object toReturn;
            //0 return values
            if(result.length == 0)
            {
                toReturn = result;
            }
            //1 return values
            else if(result.length == 1)
            {
                toReturn = validateAssignability(result[0], info.returnTypes[0], info.returnTypeParameters[0]);
            }
            //2 or more return values
            else
            {
                for(int i = 0; i < result.length; i++)
                {
                    result[i] = validateAssignability(result[i], info.returnTypes[i], info.returnTypeParameters[0]);
                }
                
                toReturn = MatlabReturns.getMaxReturn(result);
            }
            
            return toReturn;
        }
        
        private Object validateAssignability(Object value, Class<?> returnType, Class<?>[] returnTypeParameters)
        {   
            Object toReturn;
            if(value == null)
            {
                if(returnType.isPrimitive())
                {
                    throw new UnassignableReturnException("Return type is primitive and cannot be assigned null\n" +
                            "Return type: " + toClassString(returnType, returnTypeParameters));
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
                    throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
                            "Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
                            "Value type:  " + toClassString(value));
                }
            }
            else
            {
                if(!returnType.isAssignableFrom(value.getClass()))
                {
                    throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
                            "Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
                            "Value type:  " + toClassString(value));
                }
                
                //If the return type is a MatlabNumberArray subclass
                if(MatlabNumberArray.class.isAssignableFrom(returnType))
                {
                    //Check the parameterization matches
                    if(!((MatlabNumberArray) value).getOutputArrayType().equals(returnTypeParameters[0]))
                    {
                        throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
                                "Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
                                "Value type:  " + toClassString(value));
                    }
                }
                
                toReturn = value;
            }
            
            return toReturn;
        }
        
        private static String toClassString(Class<?> returnType, Class<?>[] returnTypeParameters)
        {
            String classString = returnType.getCanonicalName();
            
            if(returnTypeParameters.length != 0)
            {
                classString += "<";
                
                for(int i = 0; i < returnTypeParameters.length; i++)
                {
                    classString += returnTypeParameters[i].getCanonicalName();
                    
                    if(i != returnTypeParameters.length - 1)
                    {
                        classString += ", ";
                    }
                }
                
                classString += ">";
            }
            
            return classString;
        }
        
        private static String toClassString(Object obj)
        {
            String classString;
            
            if(obj == null)
            {
                classString = "null";
            }
            else if(MatlabNumberArray.class.isAssignableFrom(obj.getClass()))
            {
                MatlabNumberArray array = (MatlabNumberArray) obj;
                classString = array.getClass().getCanonicalName() +
                              "<" + array.getOutputArrayType().getCanonicalName() + ">";
            }
            else
            {
                classString = obj.getClass().getCanonicalName();
            }
            
            return classString;
        }

        private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_AUTOBOXED;
        static
        {
            Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
            
            map.put(byte.class, Byte.class);
            map.put(short.class, Short.class);
            map.put(int.class, Integer.class);
            map.put(long.class, Long.class);
            map.put(double.class, Double.class);
            map.put(float.class, Float.class);
            map.put(boolean.class, Boolean.class);
            map.put(char.class, Character.class);
            
            PRIMITIVE_TO_AUTOBOXED = Collections.unmodifiableMap(map);
        }
    }
    
    private static class FunctionResult implements Serializable
    {
        private final Object[] returnArgs;
        private final RuntimeException thrownException;
        
        FunctionResult(Object[] returnArgs)
        {
            this.returnArgs = returnArgs;
            this.thrownException = null;
        }
        
        FunctionResult(RuntimeException thrownException)
        {
            this.returnArgs = null;
            this.thrownException = thrownException;
        }
    }
    
    private static class CustomFunctionInvocation implements MatlabThreadCallable<FunctionResult>, Serializable
    {
        private final InvocationInfo _functionInfo;
        private final Object[] _args;
        
        private CustomFunctionInvocation(InvocationInfo functionInfo, Object[] args)
        {
            _functionInfo = functionInfo;
            _args = args;
        }
        
        @Override
        public FunctionResult call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            FunctionResult result;
            try
            {
                result = new FunctionResult(this.invoke(proxy));
            }
            catch(RuntimeException e)
            {
                result = new FunctionResult(e);
            }
            
            return result;
        }

        private Object[] invoke(MatlabOperations ops) throws MatlabInvocationException
        {
            String initialDir = null;
            
            //If the function was specified as not being on MATLAB's path
            if(_functionInfo.containingDirectory != null)
            {
                //Initial directory before cding
                initialDir = (String) ops.returningFeval("pwd", 1)[0];
                
                //No need to change directory
                if(initialDir.equals(_functionInfo.containingDirectory))
                {
                    initialDir = null;
                }
                //Change directory to where the function is located
                else
                {
                    ops.feval("cd", _functionInfo.containingDirectory);
                }
            }
            
            List<String> variablesToClear = new ArrayList<String>();
            try
            {
                //Set all arguments as MATLAB variables and build a function call using those variables
                String functionStr = _functionInfo.name + "(";
                List<String> parameterNames = generateNames(ops, "param_", _args.length);
                for(int i = 0; i < _args.length; i++)
                {
                    String name = parameterNames.get(i);
                    variablesToClear.add(name);
                    
                    setReturnValue(ops, name, _args[i]);
                    
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
                    returnNames = generateNames(ops, "return_", _functionInfo.returnTypes.length);
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
                            variablesToClear.add(name);
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
                ops.eval(functionStr);
                
                //Get the return values
                List<String> variablesToKeep = new ArrayList<String>();
                Object[] returnValues = new Object[_functionInfo.returnTypes.length];
                for(int i = 0; i < returnValues.length; i++)
                {
                    ClassInfo returnInfo = ClassInfo.getInfo(_functionInfo.returnTypes[i]);
                    
                    //No return
                    if(returnInfo.isVoid)
                    {
                        returnValues[i] = null;
                    }
                    //Not a true "return", keeps the value in MATLAB and returns the name of the variable
                    else if(_functionInfo.returnTypes[i].equals(MatlabVariable.class))
                    {
                        MatlabVariableGetter getter = new MatlabVariableGetter();
                        getter.getInMatlab(ops, returnNames.get(i));
                        returnValues[i] = getter;
                        
                        variablesToKeep.add(returnNames.get(i));
                    }
                    //Retrieve the value
                    else
                    {
                        returnValues[i] = getReturnValue(ops, returnNames.get(i), returnInfo,
                                _functionInfo.returnTypeParameters[i], variablesToClear);
                    }
                }
                
                //Do this last, so if any exceptions occurred then the variables will not be kept
                variablesToClear.removeAll(variablesToKeep);
                
                return returnValues;
            }
            //Restore MATLAB's state to what it was before the function call happened
            finally
            {
                try
                {
                    //Clear all variables used
                    if(!variablesToClear.isEmpty())
                    {
                        String clearCmd = "clear ";
                        for(int i = 0; i < variablesToClear.size(); i++)
                        {
                            clearCmd += variablesToClear.get(i);

                            if(i != variablesToClear.size() - 1)
                            {
                                clearCmd += " ";
                            }
                        }
                        ops.eval(clearCmd);
                    }
                }
                finally
                {
                    //If necessary, change back to the directory MATLAB was in before the function was invoked
                    if(initialDir != null)
                    {
                        ops.feval("cd", initialDir);
                    }
                }
            }
        }
        
        private static void setReturnValue(MatlabOperations ops, String name, Object arg)
                throws MatlabInvocationException
        {
            if(arg == null)
            {
                ops.eval(name + " = [];");
            }
            else if(arg instanceof MatlabTypeSetter)
            {
                ((MatlabTypeSetter) arg).setInMatlab(ops, name);
            }
            else if(ClassInfo.getInfo(arg.getClass()).isBuiltinNumeric)
            {   
                Number number = (Number) arg;
                
                if(number instanceof Byte)
                {
                    ops.eval(name + "=int8(" + number.byteValue() + ");");
                }
                else if(number instanceof Short)
                {
                    ops.eval(name + "=int16(" + number.shortValue() + ");");
                }
                else if(number instanceof Integer)
                {
                    ops.eval(name + "=int32(" + number.intValue() + ");");
                }
                else if(number instanceof Long)
                {
                    ops.eval(name + "=int64(" + number.longValue() + ");");
                }
                else if(number instanceof Float)
                {
                    ops.setVariable(name, new float[] { number.floatValue() });
                }
                else if(number instanceof Double)
                {
                    ops.setVariable(name, new double[] { number.doubleValue() });
                }
            }
            else
            {
                MatlabValueSetter.setInMatlab(ops, name, arg);
            }
        }
        
        private static class MatlabValueSetter
        {
            private static void setInMatlab(MatlabOperations ops, String variableName, Object value)
                    throws MatlabInvocationException
            {
                MatlabValueSetter setter = new MatlabValueSetter(value);
                ops.setVariable(variableName, setter);
                ops.eval(variableName + " = " + variableName + ".getValue();");
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
        
        private static Object getReturnValue(MatlabOperations ops, String returnName, ClassInfo returnInfo,
                Class<?>[] returnParams, List<String> variablesToClear) throws MatlabInvocationException
        {
            Object returnValue;
            
            //Empty array, MATLAB's rough equivalent of null
            if(isFoo(ops, "isempty", returnName))
            {
                returnValue = null;
            }
            //The variable is a Java object
            else if(isFoo(ops, "isjava", returnName))
            {
                returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
            }
            else
            {
                String type = (String) ops.returningEval("class(" + returnName + ");", 1)[0];

                if(type.equals("function_handle"))
                {
                    MatlabFunctionHandleGetter getter = new MatlabFunctionHandleGetter();
                    getter.getInMatlab(ops, returnName);
                    returnValue = getter;
                }
                else if(MATLAB_TO_JAVA_PRIMITIVE.containsKey(type))
                {   
                    //If a singular value
                    boolean isScalar = isFoo(ops, "isscalar", returnName);

                    //Whether the value should be returned as a linear array instead of MATLAB's default of the minimum
                    //array dimension being 2
                    boolean keepLinear = false;
                    if(!isScalar)
                    {
                        if(MatlabNumberArray.class.isAssignableFrom(returnInfo.describedClass))
                        {
                            ClassInfo returnParamInfo = ClassInfo.getInfo(returnParams[0]);
                            keepLinear = returnParamInfo.arrayDimensions == 1 && isFoo(ops, "isvector", returnName);
                        }
                        else
                        {
                            keepLinear = returnInfo.arrayDimensions == 1 && isFoo(ops, "isvector", returnName);
                        }
                    }
                    
                    //logical -> boolean
                    if(type.equals("logical"))
                    {
                        if(isScalar)
                        {
                            returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
                        }
                        else
                        {   
                            PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                            getter.getInMatlab(ops, returnName);
                            returnValue = getter;
                        }
                    }
                    //char -> char or String
                    else if(type.equals("char"))
                    {
                        //If the return type is specified as char, Character, or an array of char
                        if(char.class.equals(returnInfo.describedClass) || Character.class.equals(returnInfo.describedClass) ||
                           char.class.equals(returnInfo.baseComponentType))
                        {
                            if(isScalar)
                            {
                                returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
                            }
                            else
                            {   
                                PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                                getter.getInMatlab(ops, returnName);
                                returnValue = getter;
                            }
                        }
                        //By default retrieve it as a String or an array of Strings
                        else
                        {
                            returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
                        }
                    }
                    //Numerics
                    //int8 -> byte, int16 -> short, int32 -> int, int64 -> long, single -> float, double -> double
                    else
                    {   
                        boolean isReal = isFoo(ops, "isreal", returnName);
                        
                        //Singular value
                        if(isScalar)
                        {
                            //If the return value is real and the return type is a primitive numeric or the autobox
                            if(isReal && returnInfo.isBuiltinNumeric)
                            {
                                returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
                            }
                            //By default, return a MatlabNumber
                            else
                            {
                                MatlabNumberGetter getter = new MatlabNumberGetter();
                                getter.getInMatlab(ops, returnName);
                                returnValue = getter;
                            }
                        }
                        //Array
                        else
                        {
                            //If the return value is a real array and the return type is a primitive numeric array
                            if(isReal && returnInfo.isArray && returnInfo.baseComponentType.isPrimitive() && 
                               ClassInfo.getInfo(returnInfo.baseComponentType).isBuiltinNumeric)
                            {
                                PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
                                getter.getInMatlab(ops, returnName);
                                returnValue = getter;
                            }
                            //By default, return a MatlabNumberArray
                            else
                            {  
                                MatlabNumberArrayGetter getter = new MatlabNumberArrayGetter(keepLinear);
                                getter.getInMatlab(ops, returnName);
                                returnValue = getter;
                            }
                        }
                    }
                }
                else
                {
                    throw new UnsupportedReturnException("Unsupported MATLAB type: " + type);
                }
            }
            
            return returnValue;
        }
        
        /**
         * Convenience method to invoke a MATLAB "is" function; equivalent to {@code function(var);}
         * 
         * @param ops
         * @param function
         * @param var
         * @return
         * @throws MatlabInvocationException 
         */
        private static boolean isFoo(MatlabOperations ops, String function, String var) throws MatlabInvocationException
        {
            return ((boolean[]) ops.returningEval(function + "(" + var + ");", 1)[0])[0];
        }
        
        private static final Map<String, Class<?>> MATLAB_TO_JAVA_PRIMITIVE;
        static
        {
            Map<String, Class<?>> map = new HashMap<String, Class<?>>();
            
            map.put("int8", byte.class);
            map.put("int16", short.class);
            map.put("int32", int.class);
            map.put("int64", long.class);
            map.put("single", float.class);
            map.put("double", double.class);
            map.put("logical", boolean.class);
            map.put("char", char.class);
            
            MATLAB_TO_JAVA_PRIMITIVE = Collections.unmodifiableMap(map);
        }
        
        private static class MatlabValueReceiver
        {
            private static Object receiveValue(MatlabOperations ops, List<String> variablesToClear, String variableName)
                    throws MatlabInvocationException
            {
                String receiverName = (String) ops.returningEval("genvarname('receiver_', who);", 1)[0];
                MatlabValueReceiver receiver = new MatlabValueReceiver();
                ops.setVariable(receiverName, receiver);
                variablesToClear.add(receiverName);
                ops.eval(receiverName + ".set(" + variableName + ");");
                
                return receiver._value;
            }
            
            private Object _value = null;
            
            public void set(Object val)
            {
                _value = val;
            }
        }
        
        private List<String> generateNames(MatlabOperations ops, String root, int amount)
                throws MatlabInvocationException
        {
            //Build set of currently taken names
            Set<String> takenNames = new HashSet<String>(Arrays.asList((String[]) ops.returningEval("who", 1)[0]));
            
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