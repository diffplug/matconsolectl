/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabOperations;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;
import matlabcontrol.link.ArrayMultidimensionalizer.PrimitiveArrayGetter;
import matlabcontrol.link.MatlabFunctionHandle.MatlabFunctionHandleGetter;
import matlabcontrol.link.MatlabNumber.MatlabNumberGetter;
import matlabcontrol.link.MatlabNumberArray.MatlabNumberArrayGetter;
import matlabcontrol.link.MatlabType.MatlabTypeGetter;
import matlabcontrol.link.MatlabType.MatlabTypeSetter;
import matlabcontrol.link.MatlabVariable.MatlabVariableGetter;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Linker {
	private Linker() {}

	/**************************************************************************************************************\
	|*                                        Linking & Validation                                                *|
	\**************************************************************************************************************/

	static MatlabOperations getLinkedMatlabOperations(MatlabProxy proxy) {
		if (proxy == null) {
			throw new NullPointerException("proxy may not be null");
		}

		Class<?> opsClass = MatlabOperations.class;
		MatlabOperations operations = (MatlabOperations) Proxy.newProxyInstance(opsClass.getClassLoader(),
				new Class<?>[]{opsClass},
				new MatlabFunctionInvocationHandler(proxy, opsClass, new ConcurrentHashMap<Method, InvocationInfo>()));

		return operations;
	}

	public static <T> T link(Class<T> functionInterface, MatlabProxy proxy) {
		if (!functionInterface.isInterface()) {
			throw new LinkingException(functionInterface.getCanonicalName() + " is not an interface");
		}

		if (proxy == null) {
			throw new NullPointerException("proxy may not be null");
		}

		//Maps each method to information about the function and method
		ConcurrentMap<Method, InvocationInfo> resolvedInfo = new ConcurrentHashMap<Method, InvocationInfo>();

		//Validate and retrieve information about all of the methods in the interface
		for (Method method : functionInterface.getMethods()) {
			//Check method is annotated with function information
			MatlabFunction annotation = method.getAnnotation(MatlabFunction.class);
			if (annotation == null) {
				throw new LinkingException(method + " is not annotated with " +
						MatlabFunction.class.getCanonicalName());
			}

			//Check method can throw MatlabInvocationException
			checkExceptions(method);

			//Store information about how to invoke the method, performing validation in the process
			resolvedInfo.put(method, InvocationInfo.getInvocationInfo(method, annotation));
		}

		T functionProxy = (T) Proxy.newProxyInstance(functionInterface.getClassLoader(),
				new Class<?>[]{functionInterface},
				new MatlabFunctionInvocationHandler(proxy, functionInterface, resolvedInfo));

		return functionProxy;
	}

	private static void checkExceptions(Method method) {
		boolean assignable = false;
		Type[] genericExceptions = method.getGenericExceptionTypes();
		for (Type exception : genericExceptions) {
			if (exception instanceof Class && ((Class<?>) exception).isAssignableFrom(MatlabInvocationException.class)) {
				assignable = true;
			}
		}

		if (!assignable) {
			throw new LinkingException(method + " is not capable of throwing " +
					MatlabInvocationException.class.getCanonicalName() + " or does so with generics");
		}
	}

	/**************************************************************************************************************\
	|*                                        Function Invocation                                                 *|
	\**************************************************************************************************************/

	private static class MatlabFunctionInvocationHandler implements InvocationHandler {
		private final MatlabProxy _proxy;
		private final Class<?> _interface;
		private final ConcurrentMap<Method, InvocationInfo> _invocationsInfo;

		private MatlabFunctionInvocationHandler(MatlabProxy proxy, Class<?> functionInterface,
				ConcurrentMap<Method, InvocationInfo> invocationInfo) {
			_proxy = proxy;
			_interface = functionInterface;
			_invocationsInfo = invocationInfo;
		}

		@Override
		public Object invoke(Object o, Method method, Object[] args) throws MatlabInvocationException {
			Object result;

			//Method belongs to java.lang.Object
			if (method.getDeclaringClass().equals(Object.class)) {
				result = invokeObjectMethod(method, args);
			}
			//Methods defined in matlabcontrol.MatlabOperations interface
			else if (method.getDeclaringClass().equals(MatlabOperations.class)) {
				result = invokeMatlabOperationsMethod(method, args);
			}
			//Method belongs to the interface supplied to this linker
			else {
				result = invokeUserInterfaceMethod(method, args);
			}

			return result;
		}

		private Object invokeObjectMethod(Method method, Object[] args) {
			Object result;

			//public void String toString()
			if (method.getName().equals("toString")) {
				result = "[Linked " + _interface.getCanonicalName() + " info=" + _invocationsInfo + "]";
			}
			//public boolean equals(Object other)
			else if (method.getName().equals("equals")) {
				Object other = args[0];
				if (other != null && Proxy.isProxyClass(other.getClass()) &&
						(Proxy.getInvocationHandler(other) instanceof MatlabFunctionInvocationHandler)) {
					InvocationHandler handler = Proxy.getInvocationHandler(other);
					result = _interface.equals(((MatlabFunctionInvocationHandler) handler)._interface);
				} else {
					result = false;
				}
			}
			//public int hashCode()
			else if (method.getName().equals("hashCode")) {
				result = _interface.hashCode();
			}
			//The other methods of java.lang.Object should not be sent to this invocation handler
			else {
				throw new UnsupportedOperationException(method + " not supported");
			}

			return result;
		}

		private Object invokeMatlabOperationsMethod(Method method, Object[] args) throws MatlabInvocationException {
			Class<?> methodReturn = method.getReturnType();
			Object result;

			if (method.getName().equals("eval")) {
				InvocationInfo info = new InvocationInfo("eval", null, new Class<?>[0], new Class<?>[0][0]);

				result = invokeMatlabFunction(info, false, args, methodReturn);
			} else if (method.getName().equals("returningEval")) {
				//Each return type
				int nargout = (Integer) args[1];
				Class<?>[] returnTypes = new Class<?>[nargout];
				Arrays.fill(returnTypes, Object.class);

				InvocationInfo info = new InvocationInfo("eval", null, returnTypes, new Class<?>[nargout][0]);

				result = invokeMatlabFunction(info, false, new Object[]{args[0]}, methodReturn);
			} else if (method.getName().equals("feval")) {
				//Make function arguments at the same level as the function name
				Object[] functionArgs = (Object[]) args[1];
				Object[] firstLevelArgs = new Object[functionArgs.length + 1];
				firstLevelArgs[0] = args[0];
				System.arraycopy(functionArgs, 0, firstLevelArgs, 1, functionArgs.length);

				InvocationInfo info = new InvocationInfo("feval", null, new Class<?>[0], new Class<?>[0][0]);

				result = invokeMatlabFunction(info, false, firstLevelArgs, methodReturn);
			} else if (method.getName().equals("returningFeval")) {
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
			} else if (method.getName().equals("getVariable")) {
				InvocationInfo info = new InvocationInfo("eval", null, new Class<?>[]{Object.class}, new Class<?>[1][0]);

				result = ((Object[]) invokeMatlabFunction(info, false, args, methodReturn))[0];
			} else if (method.getName().equals("setVariable")) {
				InvocationInfo info = new InvocationInfo("assignin", null, new Class<?>[0], new Class<?>[0][0]);

				result = invokeMatlabFunction(info, false, new Object[]{"base", args[0], args[1]}, methodReturn);
			} else {
				throw new UnsupportedOperationException(method + " not supported");
			}

			return result;
		}

		private Object invokeUserInterfaceMethod(Method method, Object[] args) throws MatlabInvocationException {
			InvocationInfo info = _invocationsInfo.get(method);

			if (args == null) {
				args = new Object[0];
			}

			//If the method uses var args then we need to make those arguments first level
			if (method.isVarArgs() && args.length > 0) {
				Object varArgs = args[args.length - 1];
				if (varArgs.getClass().isArray()) {
					//Number of variable arguments
					int varArgLength = Array.getLength(varArgs);

					//Combined arguments, ignoring the last argument which was the var arg array container
					Object[] allArgs = new Object[args.length + varArgLength - 1];
					System.arraycopy(args, 0, allArgs, 0, args.length - 1);

					//Update information with var args as first level arguments
					for (int i = 0; i < varArgLength; i++) {
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
						throws MatlabInvocationException {
			//Replace all arguments with parameters of a MatlabType subclass or a multidimensional primitive array with
			//their serialized setters
			for (int i = 0; i < args.length; i++) {
				if (args[i] != null) {
					ClassInfo argInfo = ClassInfo.getInfo(args[i].getClass());

					if (argInfo.isMatlabType) {
						args[i] = ((MatlabType) args[i]).getSetter();
					} else if (argInfo.isPrimitiveArray) {
						args[i] = ArrayLinearizer.getSetter(args[i]);
					}
				}
			}

			//Invoke function
			FunctionResult result = _proxy.invokeAndWait(new CustomFunctionInvocation(info, args));
			if (result.thrownException != null) {
				result.thrownException.fillInStackTrace();
				throw result.thrownException;
			}
			Object[] returnValues = result.returnArgs;

			//For each returned value that is a serialized getter, retrieve it
			for (int i = 0; i < returnValues.length; i++) {
				if (returnValues[i] instanceof MatlabTypeGetter) {
					returnValues[i] = ((MatlabType.MatlabTypeGetter) returnValues[i]).retrieve();
				}
			}

			//Process returned values if user defined
			Object toReturn;
			if (userDefined) {
				toReturn = processReturnValues(info, returnValues, methodReturn);
			} else {
				toReturn = returnValues;
			}

			return toReturn;
		}

		private Object processReturnValues(InvocationInfo info, Object[] result, Class<?> methodReturn) {
			Object toReturn;
			//0 return values
			if (result.length == 0) {
				toReturn = result;
			}
			//1 return values
			else if (result.length == 1) {
				toReturn = validateAssignability(result[0], info.returnTypes[0], info.returnTypeParameters[0]);
			}
			//2 or more return values
			else {
				for (int i = 0; i < result.length; i++) {
					result[i] = validateAssignability(result[i], info.returnTypes[i], info.returnTypeParameters[0]);
				}

				toReturn = MatlabReturns.getMaxReturn(result);
			}

			return toReturn;
		}

		private Object validateAssignability(Object value, Class<?> returnType, Class<?>[] returnTypeParameters) {
			Object toReturn;
			if (value == null) {
				if (returnType.isPrimitive()) {
					throw new UnassignableReturnException("Return type is primitive and cannot be assigned null\n" +
							"Return type: " + toClassString(returnType, returnTypeParameters));
				} else {
					toReturn = null;
				}
			} else if (returnType.isPrimitive()) {
				Class<?> autoboxedClass = PRIMITIVE_TO_AUTOBOXED.get(returnType);
				if (autoboxedClass.equals(value.getClass())) {
					toReturn = value;
				} else {
					throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
							"Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
							"Value type:  " + toClassString(value));
				}
			} else {
				if (!returnType.isAssignableFrom(value.getClass())) {
					throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
							"Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
							"Value type:  " + toClassString(value));
				}

				//If the return type is a MatlabNumberArray subclass
				if (MatlabNumberArray.class.isAssignableFrom(returnType)) {
					//Check the parameterization matches
					if (!((MatlabNumberArray) value).getOutputArrayType().equals(returnTypeParameters[0])) {
						throw new UnassignableReturnException("Return type cannot be assigned the value returned\n" +
								"Return type: " + toClassString(returnType, returnTypeParameters) + "\n" +
								"Value type:  " + toClassString(value));
					}
				}

				toReturn = value;
			}

			return toReturn;
		}

		private static String toClassString(Class<?> returnType, Class<?>[] returnTypeParameters) {
			StringBuilder classString = new StringBuilder();
			classString.append(returnType.getCanonicalName());

			if (returnTypeParameters.length != 0) {
				classString.append("<");

				for (int i = 0; i < returnTypeParameters.length; i++) {
					classString.append(returnTypeParameters[i].getCanonicalName());

					if (i != returnTypeParameters.length - 1) {
						classString.append(", ");
					}
				}

				classString.append(">");
			}

			return classString.toString();
		}

		private static String toClassString(Object obj) {
			String classString;

			if (obj == null) {
				classString = "null";
			} else if (MatlabNumberArray.class.isAssignableFrom(obj.getClass())) {
				MatlabNumberArray array = (MatlabNumberArray) obj;
				classString = array.getClass().getCanonicalName() +
						"<" + array.getOutputArrayType().getCanonicalName() + ">";
			} else {
				classString = obj.getClass().getCanonicalName();
			}

			return classString;
		}

		private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_AUTOBOXED;

		static {
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

	private static class FunctionResult implements Serializable {
		private static final long serialVersionUID = -5636384730122824487L;
		private final Object[] returnArgs;
		private final RuntimeException thrownException;

		FunctionResult(Object[] returnArgs) {
			this.returnArgs = returnArgs;
			this.thrownException = null;
		}

		FunctionResult(RuntimeException thrownException) {
			this.returnArgs = null;
			this.thrownException = thrownException;
		}
	}

	private static class CustomFunctionInvocation implements MatlabThreadCallable<FunctionResult>, Serializable {
		private static final long serialVersionUID = -7108052527648813829L;
		private final InvocationInfo _functionInfo;
		private final Object[] _args;

		private CustomFunctionInvocation(InvocationInfo functionInfo, Object[] args) {
			_functionInfo = functionInfo;
			_args = args;
		}

		@Override
		public FunctionResult call(MatlabThreadProxy proxy) throws MatlabInvocationException {
			FunctionResult result;
			try {
				result = new FunctionResult(this.invoke(proxy));
			} catch (RuntimeException e) {
				result = new FunctionResult(e);
			}

			return result;
		}

		private Object[] invoke(MatlabOperations ops) throws MatlabInvocationException {
			String initialDir = null;

			//If the function was specified as not being on MATLAB's path
			if (_functionInfo.containingDirectory != null) {
				//Initial directory before cding
				initialDir = (String) ops.returningFeval("pwd", 1)[0];

				//No need to change directory
				if (initialDir.equals(_functionInfo.containingDirectory)) {
					initialDir = null;
				}
				//Change directory to where the function is located
				else {
					ops.feval("cd", _functionInfo.containingDirectory);
				}
			}

			List<String> variablesToClear = new ArrayList<String>();
			try {
				//Set all arguments as MATLAB variables and build a function call using those variables
				StringBuilder functionStr = new StringBuilder();
				functionStr.append(_functionInfo.name + "(");
				List<String> parameterNames = generateNames(ops, "param_", _args.length);
				for (int i = 0; i < _args.length; i++) {
					String name = parameterNames.get(i);
					variablesToClear.add(name);

					setReturnValue(ops, name, _args[i]);

					functionStr.append(name);
					if (i != _args.length - 1) {
						functionStr.append(", ");
					}
				}
				functionStr.append(");");

				//Return arguments
				List<String> returnNames = null;
				if (_functionInfo.returnTypes.length != 0) {
					returnNames = generateNames(ops, "return_", _functionInfo.returnTypes.length);
					StringBuilder returnStr = new StringBuilder();
					returnStr.append("[");
					for (int i = 0; i < returnNames.size(); i++) {
						ClassInfo returnInfo = ClassInfo.getInfo(_functionInfo.returnTypes[i]);

						String name;
						if (returnInfo.isVoid) {
							name = "~";
						} else {
							name = returnNames.get(i);
							variablesToClear.add(name);
						}

						returnStr.append(name);
						if (i != returnNames.size() - 1) {
							returnStr.append(", ");
						}
					}
					returnStr.append("] = ");

					functionStr.insert(0, returnStr);
				}

				//Invoke the function
				ops.eval(functionStr.toString());

				//Get the return values
				List<String> variablesToKeep = new ArrayList<String>();
				Object[] returnValues = new Object[_functionInfo.returnTypes.length];
				for (int i = 0; i < returnValues.length; i++) {
					ClassInfo returnInfo = ClassInfo.getInfo(_functionInfo.returnTypes[i]);

					//No return
					if (returnInfo.isVoid) {
						returnValues[i] = null;
					}
					//Not a true "return", keeps the value in MATLAB and returns the name of the variable
					else if (_functionInfo.returnTypes[i].equals(MatlabVariable.class)) {
						MatlabVariableGetter getter = new MatlabVariableGetter();
						getter.getInMatlab(ops, returnNames.get(i));
						returnValues[i] = getter;

						variablesToKeep.add(returnNames.get(i));
					}
					//Retrieve the value
					else {
						returnValues[i] = getReturnValue(ops, returnNames.get(i), returnInfo,
								_functionInfo.returnTypeParameters[i], variablesToClear);
					}
				}

				//Do this last, so if any exceptions occurred then the variables will not be kept
				variablesToClear.removeAll(variablesToKeep);

				return returnValues;
			}
			//Restore MATLAB's state to what it was before the function call happened
			finally {
				try {
					//Clear all variables used
					if (!variablesToClear.isEmpty()) {
						StringBuilder clearCmd = new StringBuilder();
						clearCmd.append("clear ");
						for (int i = 0; i < variablesToClear.size(); i++) {
							clearCmd.append(variablesToClear.get(i));

							if (i != variablesToClear.size() - 1) {
								clearCmd.append(" ");
							}
						}
						ops.eval(clearCmd.toString());
					}
				} finally {
					//If necessary, change back to the directory MATLAB was in before the function was invoked
					if (initialDir != null) {
						ops.feval("cd", initialDir);
					}
				}
			}
		}

		private static void setReturnValue(MatlabOperations ops, String name, Object arg)
				throws MatlabInvocationException {
			if (arg == null) {
				ops.eval(name + " = [];");
			} else if (arg instanceof MatlabTypeSetter) {
				((MatlabTypeSetter) arg).setInMatlab(ops, name);
			} else if (ClassInfo.getInfo(arg.getClass()).isBuiltinNumeric) {
				Number number = (Number) arg;

				if (number instanceof Byte) {
					ops.eval(name + "=int8(" + number.byteValue() + ");");
				} else if (number instanceof Short) {
					ops.eval(name + "=int16(" + number.shortValue() + ");");
				} else if (number instanceof Integer) {
					ops.eval(name + "=int32(" + number.intValue() + ");");
				} else if (number instanceof Long) {
					ops.eval(name + "=int64(" + number.longValue() + ");");
				} else if (number instanceof Float) {
					ops.setVariable(name, new float[]{number.floatValue()});
				} else if (number instanceof Double) {
					ops.setVariable(name, new double[]{number.doubleValue()});
				}
			} else {
				MatlabValueSetter.setInMatlab(ops, name, arg);
			}
		}

		private static class MatlabValueSetter {
			private static void setInMatlab(MatlabOperations ops, String variableName, Object value)
					throws MatlabInvocationException {
				MatlabValueSetter setter = new MatlabValueSetter(value);
				ops.setVariable(variableName, setter);
				ops.eval(variableName + " = " + variableName + ".getValue();");
			}

			private final Object _value;

			private MatlabValueSetter(Object value) {
				_value = value;
			}

			@SuppressWarnings("unused")
			// called in MATLAB by the script above
			public Object getValue() {
				return _value;
			}
		}

		private static Object getReturnValue(MatlabOperations ops, String returnName, ClassInfo returnInfo,
				Class<?>[] returnParams, List<String> variablesToClear) throws MatlabInvocationException {
			Object returnValue;

			//Empty array, MATLAB's rough equivalent of null
			if (isFoo(ops, "isempty", returnName)) {
				returnValue = null;
			}
			//The variable is a Java object
			else if (isFoo(ops, "isjava", returnName)) {
				returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
			} else {
				String type = (String) ops.returningEval("class(" + returnName + ");", 1)[0];

				if (type.equals("function_handle")) {
					MatlabFunctionHandleGetter getter = new MatlabFunctionHandleGetter();
					getter.getInMatlab(ops, returnName);
					returnValue = getter;
				} else if (MATLAB_TO_JAVA_PRIMITIVE.containsKey(type)) {
					//If a singular value
					boolean isScalar = isFoo(ops, "isscalar", returnName);

					//Whether the value should be returned as a linear array instead of MATLAB's default of the minimum
					//array dimension being 2
					boolean keepLinear = false;
					if (!isScalar) {
						if (MatlabNumberArray.class.isAssignableFrom(returnInfo.describedClass)) {
							ClassInfo returnParamInfo = ClassInfo.getInfo(returnParams[0]);
							keepLinear = returnParamInfo.arrayDimensions == 1 && isFoo(ops, "isvector", returnName);
						} else {
							keepLinear = returnInfo.arrayDimensions == 1 && isFoo(ops, "isvector", returnName);
						}
					}

					//logical -> boolean
					if (type.equals("logical")) {
						if (isScalar) {
							returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
						} else {
							PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
							getter.getInMatlab(ops, returnName);
							returnValue = getter;
						}
					}
					//char -> char or String
					else if (type.equals("char")) {
						//If the return type is specified as char, Character, or an array of char
						if (char.class.equals(returnInfo.describedClass) || Character.class.equals(returnInfo.describedClass) ||
								char.class.equals(returnInfo.baseComponentType)) {
							if (isScalar) {
								returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
							} else {
								PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
								getter.getInMatlab(ops, returnName);
								returnValue = getter;
							}
						}
						//By default retrieve it as a String or an array of Strings
						else {
							returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
						}
					}
					//Numerics
					//int8 -> byte, int16 -> short, int32 -> int, int64 -> long, single -> float, double -> double
					else {
						boolean isReal = isFoo(ops, "isreal", returnName);

						//Singular value
						if (isScalar) {
							//If the return value is real and the return type is a primitive numeric or the autobox
							if (isReal && returnInfo.isBuiltinNumeric) {
								returnValue = MatlabValueReceiver.receiveValue(ops, variablesToClear, returnName);
							}
							//By default, return a MatlabNumber
							else {
								MatlabNumberGetter getter = new MatlabNumberGetter();
								getter.getInMatlab(ops, returnName);
								returnValue = getter;
							}
						}
						//Array
						else {
							//If the return value is a real array and the return type is a primitive numeric array
							if (isReal && returnInfo.isArray && returnInfo.baseComponentType.isPrimitive() &&
									ClassInfo.getInfo(returnInfo.baseComponentType).isBuiltinNumeric) {
								PrimitiveArrayGetter getter = new PrimitiveArrayGetter(true, keepLinear);
								getter.getInMatlab(ops, returnName);
								returnValue = getter;
							}
							//By default, return a MatlabNumberArray
							else {
								MatlabNumberArrayGetter getter = new MatlabNumberArrayGetter(keepLinear);
								getter.getInMatlab(ops, returnName);
								returnValue = getter;
							}
						}
					}
				} else {
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
		private static boolean isFoo(MatlabOperations ops, String function, String var) throws MatlabInvocationException {
			return ((boolean[]) ops.returningEval(function + "(" + var + ");", 1)[0])[0];
		}

		private static final Map<String, Class<?>> MATLAB_TO_JAVA_PRIMITIVE;

		static {
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

		private static class MatlabValueReceiver {
			private static Object receiveValue(MatlabOperations ops, List<String> variablesToClear, String variableName)
					throws MatlabInvocationException {
				String receiverName = (String) ops.returningEval("genvarname('receiver_', who);", 1)[0];
				MatlabValueReceiver receiver = new MatlabValueReceiver();
				ops.setVariable(receiverName, receiver);
				variablesToClear.add(receiverName);
				ops.eval(receiverName + ".set(" + variableName + ");");

				return receiver._value;
			}

			private Object _value = null;

			@SuppressWarnings("unused")
			// called in MATLAB by the script above
			public void set(Object val) {
				_value = val;
			}
		}

		private List<String> generateNames(MatlabOperations ops, String root, int amount)
				throws MatlabInvocationException {
			//Build set of currently taken names
			Set<String> takenNames = new HashSet<String>(Arrays.asList((String[]) ops.returningEval("who", 1)[0]));

			//Generate names
			List<String> generatedNames = new ArrayList<String>();
			int genSequenence = 0;
			while (generatedNames.size() != amount) {
				String generatedName = root + genSequenence;
				while (takenNames.contains(generatedName)) {
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
