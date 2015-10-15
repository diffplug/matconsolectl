/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.lang.reflect.Array;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps around a {@link MatlabProxy} to provide a log of interactions. The data is not altered. This logger is useful
 * for determining the Java types and structure of data returned from MATLAB.
 * <br><br>
 * Interaction with all methods, except those defined in {@code Object} and not overridden, is logged. Entering a
 * method, exiting a method, and throwing an exception are logged. Method parameters and return values are logged. The
 * contents of a returned array will be recursively explored and its contents logged. As is convention, all of these
 * interactions are logged at {@code Level.FINER}. If the logging system has not been otherwise configured, then the
 * {@code ConsoleHandler} which prints log messages to the console will not show these log messages as their level is
 * too low. To configure the {@code ConsoleHandler} to show these log messages, call {@link #showInConsoleHandler()}.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class LoggingMatlabProxy extends MatlabProxy {
	private static final String CLASS_NAME = LoggingMatlabProxy.class.getName();

	private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

	static {
		LOGGER.setLevel(Level.FINER);
	}

	private final MatlabProxy _delegate;

	/**
	 * Constructs the logging proxy. All methods defined in {@code MatlabProxy} will be delegated to
	 * {@code delegateProxy}.
	 * 
	 * @param delegateProxy
	 */
	public LoggingMatlabProxy(MatlabProxy delegateProxy) {
		super(delegateProxy.getIdentifier(), delegateProxy.isExistingSession());

		_delegate = delegateProxy;
	}

	/**
	 * Configures the {@code ConsoleHandler} responsible for showing logging records to show the records that are
	 * logged by this proxy. This behavior is useful if you have not otherwise configured logging in your application.
	 */
	public static void showInConsoleHandler() {
		for (Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(Level.FINER);
			}
		}
	}

	private static abstract class Invocation {
		final String name;
		final Object[] args;

		public Invocation(String name, Object... args) {
			this.name = name;
			this.args = args;
		}
	}

	private static abstract class VoidThrowingInvocation extends Invocation {
		public VoidThrowingInvocation(String name, Object... args) {
			super(name, args);
		}

		public abstract void invoke() throws MatlabInvocationException;
	}

	private static abstract class VoidInvocation extends Invocation {
		public VoidInvocation(String name, Object... args) {
			super(name, args);
		}

		public abstract void invoke();
	}

	private static abstract class ReturnThrowingInvocation<T> extends Invocation {
		public ReturnThrowingInvocation(String name, Object... args) {
			super(name, args);
		}

		public abstract T invoke() throws MatlabInvocationException;
	}

	private static abstract class ReturnInvocation<T> extends Invocation {
		public ReturnInvocation(String name, Object... args) {
			super(name, args);
		}

		public abstract T invoke();
	}

	private static abstract class ReturnBooleanInvocation extends Invocation {
		public ReturnBooleanInvocation(String name, Object... args) {
			super(name, args);
		}

		public abstract boolean invoke();
	}

	private void invoke(VoidThrowingInvocation invocation) throws MatlabInvocationException {
		LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);

		try {
			invocation.invoke();
			LOGGER.exiting(CLASS_NAME, invocation.name);
		} catch (MatlabInvocationException e) {
			LOGGER.throwing(CLASS_NAME, invocation.name, e);
			LOGGER.exiting(CLASS_NAME, invocation.name);

			throw e;
		}
	}

	private void invoke(VoidInvocation invocation) {
		LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
		invocation.invoke();
		LOGGER.exiting(CLASS_NAME, invocation.name);
	}

	private <T> T invoke(ReturnThrowingInvocation<T> invocation) throws MatlabInvocationException {
		T data;

		LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
		try {
			data = invocation.invoke();
			LOGGER.exiting(CLASS_NAME, invocation.name, formatResult(data));
		} catch (MatlabInvocationException e) {
			LOGGER.throwing(CLASS_NAME, invocation.name, e);
			LOGGER.exiting(CLASS_NAME, invocation.name);

			throw e;
		}

		return data;
	}

	private <T> T invoke(ReturnInvocation<T> invocation) {
		LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
		T data = invocation.invoke();
		LOGGER.exiting(CLASS_NAME, invocation.name, formatResult(data));

		return data;
	}

	private boolean invoke(ReturnBooleanInvocation invocation) {
		LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
		boolean data = invocation.invoke();
		LOGGER.exiting(CLASS_NAME, invocation.name, "boolean: " + data);

		return data;
	}

	@Override
	public void eval(final String command) throws MatlabInvocationException {
		this.invoke(new VoidThrowingInvocation("eval(String)", command) {
			@Override
			public void invoke() throws MatlabInvocationException {
				_delegate.eval(command);
			}
		});
	}

	@Override
	public Object[] returningEval(final String command, final int nargout) throws MatlabInvocationException {
		return this.invoke(new ReturnThrowingInvocation<Object[]>("returningEval(String, int)", command, nargout) {
			@Override
			public Object[] invoke() throws MatlabInvocationException {
				return _delegate.returningEval(command, nargout);
			}
		});
	}

	@Override
	public void feval(final String functionName, final Object... args) throws MatlabInvocationException {
		this.invoke(new VoidThrowingInvocation("feval(String, Object...)", functionName, args) {
			@Override
			public void invoke() throws MatlabInvocationException {
				_delegate.feval(functionName, args);
			}
		});
	}

	@Override
	public Object[] returningFeval(final String functionName, final int nargout, final Object... args)
			throws MatlabInvocationException {
		return this.invoke(new ReturnThrowingInvocation<Object[]>("returningFeval(String, int, Object...)",
				functionName, nargout, args) {
			@Override
			public Object[] invoke() throws MatlabInvocationException {
				return _delegate.returningFeval(functionName, nargout, args);
			}
		});
	}

	@Override
	public void setVariable(final String variableName, final Object value) throws MatlabInvocationException {
		this.invoke(new VoidThrowingInvocation("setVariable(String, int)", variableName, value) {
			@Override
			public void invoke() throws MatlabInvocationException {
				_delegate.setVariable(variableName, value);
			}
		});
	}

	@Override
	public Object getVariable(final String variableName) throws MatlabInvocationException {
		return this.invoke(new ReturnThrowingInvocation<Object>("getVariable(String)", variableName) {
			@Override
			public Object invoke() throws MatlabInvocationException {
				return _delegate.getVariable(variableName);
			}
		});
	}

	@Override
	public <U> U invokeAndWait(final MatlabThreadCallable<U> callable) throws MatlabInvocationException {
		return this.invoke(new ReturnThrowingInvocation<U>("invokeAndWait(MatlabThreadCallable)", callable) {
			@Override
			public U invoke() throws MatlabInvocationException {
				return _delegate.invokeAndWait(callable);
			}
		});
	}

	@Override
	public void addDisconnectionListener(final DisconnectionListener listener) {
		this.invoke(new VoidInvocation("addDisconnectionListener(DisconnectionListener)", listener) {
			@Override
			public void invoke() {
				_delegate.addDisconnectionListener(listener);
			}
		});

	}

	@Override
	public void removeDisconnectionListener(final DisconnectionListener listener) {
		this.invoke(new VoidInvocation("removeDisconnectionListener(DisconnectionListener)", listener) {
			@Override
			public void invoke() {
				_delegate.removeDisconnectionListener(listener);
			}
		});
	}

	@Override
	public boolean disconnect() {
		return this.invoke(new ReturnBooleanInvocation("disconnect()") {
			@Override
			public boolean invoke() {
				return _delegate.disconnect();
			}
		});
	}

	@Override
	public boolean isExistingSession() {
		return this.invoke(new ReturnBooleanInvocation("isExistingSession()") {
			@Override
			public boolean invoke() {
				return _delegate.isExistingSession();
			}
		});
	}

	@Override
	public boolean isRunningInsideMatlab() {
		return this.invoke(new ReturnBooleanInvocation("isRunningInsideMatlab()") {
			@Override
			public boolean invoke() {
				return _delegate.isRunningInsideMatlab();
			}
		});
	}

	@Override
	public boolean isConnected() {
		return this.invoke(new ReturnBooleanInvocation("isConnected()") {
			@Override
			public boolean invoke() {
				return _delegate.isConnected();
			}
		});
	}

	@Override
	public Identifier getIdentifier() {
		return this.invoke(new ReturnInvocation<Identifier>("getIdentifier()") {
			@Override
			public Identifier invoke() {
				return _delegate.getIdentifier();
			}
		});
	}

	@Override
	public void exit() throws MatlabInvocationException {
		this.invoke(new VoidThrowingInvocation("exit()") {
			@Override
			public void invoke() throws MatlabInvocationException {
				_delegate.exit();
			}
		});
	}

	@Override
	public String toString() {
		return "[" + this.getClass().getName() + " delegateProxy=" + _delegate + "]";
	}

	private String formatResult(Object result) {
		String formattedResult;

		if (result == null) {
			formattedResult = "null";
		} else if (result.getClass().isArray()) {
			formattedResult = result.getClass().getName() + "\n" + formatResult(result, 0).trim();
		} else {
			formattedResult = result.getClass().getName() + ": " + result.toString();
		}

		return formattedResult;
	}

	/**
	 * Takes in the result from MATLAB and turns it into an easily readable format.
	 * 
	 * @param result
	 * @param level, pass in 0 to initialize, used recursively
	 * @return description
	 */
	private static String formatResult(Object result, int level) {
		//Message builder
		StringBuilder builder = new StringBuilder();

		//Tab offset for levels
		String tab = "";
		for (int i = 0; i < level + 1; i++) {
			tab += "  ";
		}

		//If the result is null
		if (result == null) {
			builder.append("null\n");
		}
		//If the result is an array
		else if (result.getClass().isArray()) {
			Class<?> componentClass = result.getClass().getComponentType();

			//Primitive array
			if (componentClass.isPrimitive()) {
				String componentName = componentClass.getCanonicalName();
				int length = Array.getLength(result);

				builder.append(componentName);
				builder.append(" array, length = ");
				builder.append(length);
				builder.append("\n");

				for (int i = 0; i < length; i++) {
					builder.append(tab);
					builder.append("index ");
					builder.append(i);
					builder.append(", ");
					builder.append(componentName);
					builder.append(": ");
					builder.append(Array.get(result, i));
					builder.append("\n");
				}
			}
			//Object array
			else {
				Object[] array = (Object[]) result;

				builder.append(array.getClass().getComponentType().getCanonicalName());
				builder.append(" array, length = ");
				builder.append(array.length);
				builder.append("\n");

				for (int i = 0; i < array.length; i++) {
					builder.append(tab);
					builder.append("index ");
					builder.append(i);
					builder.append(", ");
					builder.append(formatResult(array[i], level + 1));
				}
			}
		}
		//If an Object and not an array
		else {
			builder.append(result.getClass().getCanonicalName());
			builder.append(": ");
			builder.append(result);
			builder.append("\n");
		}

		return builder.toString();
	}
}
