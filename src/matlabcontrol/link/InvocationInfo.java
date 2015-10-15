/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A holder of information about the Java method and the associated MATLAB function.
 * 
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class InvocationInfo implements Serializable {
	private static final long serialVersionUID = 764281612994597133L;

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

	static InvocationInfo getInvocationInfo(Method method, MatlabFunction annotation) {
		FunctionInfo funcInfo = getFunctionInfo(method, annotation);
		ReturnTypeInfo returnInfo = getReturnTypes(method);
		InvocationInfo invocationInfo = new InvocationInfo(funcInfo.name, funcInfo.containingDirectory,
				returnInfo.returnTypes, returnInfo.returnTypeParameters);

		return invocationInfo;
	}

	InvocationInfo(String name, String containingDirectory, Class<?>[] returnTypes, Class<?>[][] returnTypeParameters) {
		this.name = name;
		this.containingDirectory = containingDirectory;

		this.returnTypes = returnTypes;
		this.returnTypeParameters = returnTypeParameters;
	}

	@Override
	public String toString() {
		StringBuilder genericParameters = new StringBuilder();
		genericParameters.append("[");
		for (int i = 0; i < returnTypeParameters.length; i++) {
			genericParameters.append(classArrayToString(returnTypeParameters[i]));

			if (i != returnTypeParameters.length - 1) {
				genericParameters.append(" ");
			}
		}
		genericParameters.append("]");

		return "[" + this.getClass().getSimpleName() +
				" name=" + name + "," +
				" containingDirectory=" + containingDirectory + "," +
				" returnTypes=" + classArrayToString(returnTypes) + "," +
				" returnTypesGenericParameters=" + genericParameters + "]";
	}

	private static String classArrayToString(Class<?>[] array) {
		StringBuilder str = new StringBuilder();
		str.append("[");
		for (int i = 0; i < array.length; i++) {
			str.append(array[i].getCanonicalName());

			if (i != array.length - 1) {
				str.append(" ");
			}
		}
		str.append("]");
		return str.toString();
	}

	private static class FunctionInfo {
		final String name;
		final String containingDirectory;

		public FunctionInfo(String name, String containingDirectory) {
			this.name = name;
			this.containingDirectory = containingDirectory;
		}
	}

	private static FunctionInfo getFunctionInfo(Method method, MatlabFunction annotation) {
		//Determine the function's name and if applicable, the directory the function is located in
		String functionName;
		String containingDirectory;

		//If a function name
		if (isFunctionName(annotation.value())) {
			functionName = annotation.value();
			containingDirectory = null;
		}
		//If a path
		else {
			String path = annotation.value();

			//Retrieve location of function file
			File functionFile;
			if (new File(path).isAbsolute()) {
				functionFile = new File(path);
			} else {
				functionFile = resolveRelativePath(method, path);
			}

			//Resolve canonical path
			try {
				functionFile = functionFile.getCanonicalFile();
			} catch (IOException e) {
				throw new LinkingException("Unable to resolve canonical path of specified function\n" +
						"method: " + method.getName() + "\n" +
						"path:" + path + "\n" +
						"non-canonical path: " + functionFile.getAbsolutePath(), e);
			}

			//Validate file location
			if (!functionFile.exists()) {
				throw new LinkingException("Specified file does not exist\n" +
						"method: " + method.getName() + "\n" +
						"path: " + path + "\n" +
						"resolved as: " + functionFile.getAbsolutePath());
			}
			if (!functionFile.isFile()) {
				throw new LinkingException("Specified file is not a file\n" +
						"method: " + method.getName() + "\n" +
						"path: " + path + "\n" +
						"resolved as: " + functionFile.getAbsolutePath());
			}
			if (!(functionFile.getName().endsWith(".m") || functionFile.getName().endsWith(".p"))) {
				throw new LinkingException("Specified file does not end in .m or .p\n" +
						"method: " + method.getName() + "\n" +
						"path: " + path + "\n" +
						"resolved as: " + functionFile.getAbsolutePath());
			}

			//Parse out the name of the function and the directory containing it
			containingDirectory = functionFile.getParent();
			functionName = functionFile.getName().substring(0, functionFile.getName().length() - 2);

			//Validate the function name
			if (!isFunctionName(functionName)) {
				throw new LinkingException("Specified file's name is not a MATLAB function name\n" +
						"Function Name: " + functionName + "\n" +
						"File: " + functionFile.getAbsolutePath());
			}
		}

		return new FunctionInfo(functionName, containingDirectory);
	}

	private static boolean isFunctionName(String functionName) {
		boolean isFunctionName = true;

		char[] nameChars = functionName.toCharArray();

		if (!Character.isLetter(nameChars[0])) {
			isFunctionName = false;
		} else {
			for (char element : nameChars) {
				if (!(Character.isLetter(element) || Character.isDigit(element) || element == '_')) {
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
	private static final ConcurrentHashMap<Class<?>, Map<String, File>> UNZIPPED_ENTRIES = new ConcurrentHashMap<Class<?>, Map<String, File>>();

	/**
	 * Resolves the location of {@code relativePath} relative to the interface which declared {@code method}. If the
	 * interface is inside of a zip file (jar/war/ear etc. file) then the contents of the zip file may first need to be
	 * unzipped.
	 * 
	 * @param method
	 * @param relativePath
	 * @return the absolute location of the file
	 */
	private static File resolveRelativePath(Method method, String relativePath) {
		Class<?> theInterface = method.getDeclaringClass();
		File interfaceLocation = getClassLocation(theInterface);

		File functionFile;

		//Code source is in a file, which means it should be jar/ear/war/zip etc.
		if (interfaceLocation.isFile()) {
			if (!UNZIPPED_ENTRIES.containsKey(theInterface)) {
				UnzipResult unzipResult = unzip(interfaceLocation);
				Map<String, File> mapping = unzipResult.unzippedMapping;
				List<File> filesToDelete = new ArrayList<File>(mapping.values());
				filesToDelete.add(unzipResult.rootDirectory);

				//If there was a previous mapping, delete all of the files just unzipped 
				if (UNZIPPED_ENTRIES.putIfAbsent(theInterface, mapping) != null) {
					deleteFiles(filesToDelete, true);
				}
				//No previous mapping, delete unzipped files on JVM exit
				else {
					deleteFiles(filesToDelete, false);
				}
			}

			functionFile = UNZIPPED_ENTRIES.get(theInterface).get(relativePath);

			if (functionFile == null) {
				throw new LinkingException("Unable to find file inside of zip\n" +
						"Method: " + method.getName() + "\n" +
						"Relative Path: " + relativePath + "\n" +
						"Zip File: " + interfaceLocation.getAbsolutePath());
			}
		}
		//Code source is a directory, it code should not be inside a jar/ear/war/zip etc.
		else {
			functionFile = new File(interfaceLocation, relativePath);
		}

		return functionFile;
	}

	private static void deleteFiles(Collection<File> files, boolean deleteNow) {
		ArrayList<File> sortedFiles = new ArrayList<File>(files);
		Collections.sort(sortedFiles);

		//Delete files in the opposite order so that files are deleted before their containing directories
		if (deleteNow) {
			for (int i = sortedFiles.size() - 1; i >= 0; i--) {
				boolean deleted = sortedFiles.get(i).delete();
				if (!deleted) {
					System.err.println("unable to delete " + sortedFiles.get(i));
				}
			}
		}
		//Delete files in the existing order because the files will be deleted on exit in the opposite order
		else {
			for (File file : sortedFiles) {
				file.deleteOnExit();
			}
		}
	}

	/**
	 * Cache used by {@link #getClassLocation(java.lang.Class)}}.
	 */
	private static final ConcurrentHashMap<Class<?>, File> CLASS_LOCATIONS = new ConcurrentHashMap<Class<?>, File>();

	private static File getClassLocation(Class<?> clazz) {
		File result = CLASS_LOCATIONS.get(clazz);
		if (result != null) {
			return result;
		} else {
			try {
				URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
				File file = new File(url.toURI().getPath()).getCanonicalFile();
				if (!file.exists()) {
					throw new LinkingException("Incorrectly resolved location of class\n" +
							"class: " + clazz.getCanonicalName() + "\n" +
							"location: " + file.getAbsolutePath());
				}
				return CLASS_LOCATIONS.putIfAbsent(clazz, file);
			} catch (IOException e) {
				throw new LinkingException("Unable to determine location of " + clazz.getCanonicalName(), e);
			} catch (URISyntaxException e) {
				throw new LinkingException("Unable to determine location of " + clazz.getCanonicalName(), e);
			}
		}
	}

	private static class UnzipResult {
		final Map<String, File> unzippedMapping;
		File rootDirectory;

		private UnzipResult(Map<String, File> mapping, File root) {
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
	private static UnzipResult unzip(File zipLocation) {
		ZipFile zip;
		try {
			zip = new ZipFile(zipLocation);
		} catch (IOException e) {
			throw new LinkingException("Unable to open zip file\n" +
					"zip location: " + zipLocation.getAbsolutePath(), e);
		}

		try {
			//Mapping from entry names to the unarchived location on disk
			Map<String, File> entryMap = new HashMap<String, File>();

			//Destination
			File unzipDir = new File(System.getProperty("java.io.tmpdir"), "linked_" + UUID.randomUUID().toString());

			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();

				//Directory
				if (entry.isDirectory()) {
					File destDir = new File(unzipDir, entry.getName());
					boolean mkdir = destDir.mkdirs();
					if (!mkdir) {
						System.err.println("Unable to mkdir " + destDir);
					}

					entryMap.put(entry.getName(), destDir);
				}
				//File
				else {
					//File should not exist, but confirm it
					File destFile = new File(unzipDir, entry.getName());
					if (destFile.exists()) {
						throw new LinkingException("Cannot unzip file, randomly generated path already exists\n" +
								"generated path: " + destFile.getAbsolutePath() + "\n" +
								"zip file: " + zipLocation.getAbsolutePath());
					}
					boolean mkdir = destFile.getParentFile().mkdirs();
					if (!mkdir) {
						System.err.println("Unable to mkdir " + destFile.getParentFile());
					}

					//Unarchive
					try {
						final int BUFFER_SIZE = 2048;
						OutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
						try {
							InputStream entryStream = zip.getInputStream(entry);
							try {
								byte[] buffer = new byte[BUFFER_SIZE];
								int count;
								while ((count = entryStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
									dest.write(buffer, 0, count);
								}
								dest.flush();
							} finally {
								entryStream.close();
							}
						} finally {
							dest.close();
						}
					} catch (IOException e) {
						throw new LinkingException("Unable to unzip file entry\n" +
								"entry: " + entry.getName() + "\n" +
								"zip location: " + zipLocation.getAbsolutePath() + "\n" +
								"destination file: " + destFile.getAbsolutePath(), e);
					}

					entryMap.put(entry.getName(), destFile);
				}
			}

			return new UnzipResult(Collections.unmodifiableMap(entryMap), unzipDir);
		} finally {
			try {
				zip.close();
			} catch (IOException ex) {
				throw new LinkingException("Unable to close zip file: " + zipLocation.getAbsolutePath(), ex);
			}
		}
	}

	private static class ReturnTypeInfo {
		Class<?>[] returnTypes;
		Class<?>[][] returnTypeParameters;

		private ReturnTypeInfo(Class<?>[] returnTypes, Class<?>[][] returnTypeParameters) {
			this.returnTypes = returnTypes;
			this.returnTypeParameters = returnTypeParameters;
		}
	}

	private static ReturnTypeInfo getReturnTypes(Method method) {
		//The type-erasured return type of the method
		Class<?> methodReturn = method.getReturnType();

		//The return type of the method with type information
		Type genericReturn = method.getGenericReturnType();

		//The return types to be determined
		Class<?>[] returnTypes;
		Class<?>[][] returnTypeGenericParameters;

		//0 return arguments
		if (methodReturn.equals(void.class)) {
			returnTypes = new Class<?>[0];
			returnTypeGenericParameters = new Class<?>[0][0];
		}
		//1 return argument
		else if (!MatlabReturns.ReturnN.class.isAssignableFrom(methodReturn)) {
			//MatlabNumberArray subclasses are allowed to be parameterized
			if (MatlabNumberArray.class.isAssignableFrom(methodReturn)) {
				if (genericReturn instanceof ParameterizedType) {
					Type[] parameterizedTypes = ((ParameterizedType) genericReturn).getActualTypeArguments();
					Class<?> parameter = getMatlabNumberArrayParameter(parameterizedTypes[0], methodReturn, method);
					returnTypeGenericParameters = new Class<?>[][]{{parameter}};
				} else {
					throw new LinkingException(method + " must parameterize " + methodReturn.getCanonicalName());
				}
			} else if (!methodReturn.equals(genericReturn)) {
				throw new LinkingException(method + " may not have a return type that uses generics");
			} else {
				returnTypeGenericParameters = new Class<?>[1][0];
			}

			returnTypes = new Class<?>[]{methodReturn};
		}
		//2 or more return arguments
		else {
			if (genericReturn instanceof ParameterizedType) {
				Type[] parameterizedTypes = ((ParameterizedType) genericReturn).getActualTypeArguments();
				returnTypes = new Class<?>[parameterizedTypes.length];
				returnTypeGenericParameters = new Class<?>[parameterizedTypes.length][];

				for (int i = 0; i < parameterizedTypes.length; i++) {
					Type type = parameterizedTypes[i];

					if (type instanceof Class) {
						Class<?> returnType = (Class<?>) type;
						if (MatlabNumberArray.class.isAssignableFrom(returnType)) {
							throw new LinkingException(method + " must parameterize " + returnType.getCanonicalName());
						}
						returnTypes[i] = returnType;
						returnTypeGenericParameters[i] = new Class[0];
					} else if (type instanceof GenericArrayType) {
						returnTypes[i] = getClassOfArrayType((GenericArrayType) type, method);
						returnTypeGenericParameters[i] = new Class[0];
					} else if (type instanceof ParameterizedType) {
						//MatlabNumberArray subclasses are allowed to be parameterized
						ParameterizedType parameterizedType = (ParameterizedType) type;
						Type rawType = parameterizedType.getRawType();
						if (rawType instanceof Class && MatlabNumberArray.class.isAssignableFrom((Class<?>) rawType)) {
							Class<?> returnType = (Class<?>) rawType;
							returnTypes[i] = returnType;

							Class<?> parameter = getMatlabNumberArrayParameter(
									parameterizedType.getActualTypeArguments()[0], returnType, method);
							returnTypeGenericParameters[i] = new Class[]{parameter};
						} else {
							throw new LinkingException(method + " may not parameterize " +
									methodReturn.getCanonicalName() + " with a parameterized type");
						}
					} else if (type instanceof WildcardType) {
						throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
								" with a wild card type");
					} else if (type instanceof TypeVariable) {
						throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
								" with a generic type");
					} else {
						throw new LinkingException(method + " may not parameterize " + methodReturn.getCanonicalName() +
								" with " + type);
					}
				}
			} else {
				throw new LinkingException(method + " must parameterize " + methodReturn.getCanonicalName());
			}
		}

		return new ReturnTypeInfo(returnTypes, returnTypeGenericParameters);
	}

	private static Class<?> getMatlabNumberArrayParameter(Type type, Class<?> matlabArrayClass, Method method) {
		Class<?> parameter;
		if (type instanceof GenericArrayType) {
			parameter = getClassOfArrayType((GenericArrayType) type, method);
			ClassInfo paramInfo = ClassInfo.getInfo(parameter);

			boolean validParameter = ((matlabArrayClass.equals(MatlabInt8Array.class) && byte.class.equals(paramInfo.baseComponentType)) ||
					(matlabArrayClass.equals(MatlabInt16Array.class) && short.class.equals(paramInfo.baseComponentType)) ||
					(matlabArrayClass.equals(MatlabInt32Array.class) && int.class.equals(paramInfo.baseComponentType)) ||
					(matlabArrayClass.equals(MatlabInt64Array.class) && long.class.equals(paramInfo.baseComponentType)) ||
					(matlabArrayClass.equals(MatlabSingleArray.class) && float.class.equals(paramInfo.baseComponentType)) ||
					(matlabArrayClass.equals(MatlabDoubleArray.class) && double.class.equals(paramInfo.baseComponentType)));

			if (!validParameter) {
				throw new LinkingException(method + " may not parameterize " + matlabArrayClass.getCanonicalName() +
						" with " + parameter.getCanonicalName());
			}
		} else {
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
	private static Class<?> getClassOfArrayType(GenericArrayType type, Method method) {
		int dimensions = 1;
		Type componentType = type.getGenericComponentType();
		while (!(componentType instanceof Class)) {
			dimensions++;
			if (componentType instanceof GenericArrayType) {
				componentType = ((GenericArrayType) componentType).getGenericComponentType();
			} else if (componentType instanceof TypeVariable) {
				throw new LinkingException(method + " may not parameterize " +
						method.getReturnType().getCanonicalName() + " with a generic array");
			} else {
				throw new LinkingException(method + " may not parameterize " +
						method.getReturnType().getCanonicalName() + " with an array of type " + type);
			}
		}

		return ArrayUtils.getArrayClass((Class<?>) componentType, dimensions);
	}
}
