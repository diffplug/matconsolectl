/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;

/**
 * Formats returned {@code Object}s and {@code Exception}s from MATLAB.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ReturnFormatter {
	private ReturnFormatter() {}

	/**
	 * Format the exception into a string that can be displayed.
	 * 
	 * @param exc the exception
	 * @return the exception as a string
	 */
	public static String formatException(Exception exc) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		exc.printStackTrace(printWriter);
		try {
			stringWriter.close();
		} catch (IOException ex) {}

		return stringWriter.toString();
	}

	/**
	 * Takes in the result from MATLAB and turns it into an easily readable format.
	 * 
	 * @param result
	 * @return description
	 */
	public static String formatResult(Object result) {
		return formatResult(result, 0);
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
			builder.append("null encountered\n");
		}
		//If the result is an array
		else if (result.getClass().isArray()) {
			Class<?> componentClass = result.getClass().getComponentType();

			//Primitive array
			if (componentClass.isPrimitive()) {
				String componentName = componentClass.toString();
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

				builder.append(array.getClass().getComponentType().getName());
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
