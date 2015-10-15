/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.link;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @since 4.2.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ClassInfo {
	private static ConcurrentMap<Class<?>, ClassInfo> CACHE = new ConcurrentHashMap<Class<?>, ClassInfo>();

	static ClassInfo getInfo(Class<?> clazz) {
		ClassInfo info = CACHE.get(clazz);
		if (info == null) {
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

	private ClassInfo(Class<?> clazz) {
		describedClass = clazz;

		isPrimitive = clazz.isPrimitive();

		if (clazz.isArray()) {
			isArray = true;

			int dim = 0;
			Class<?> type = clazz;
			while (type.isArray()) {
				dim++;
				type = type.getComponentType();
			}

			arrayDimensions = dim;
			baseComponentType = type;
			isPrimitiveArray = type.isPrimitive();
		} else {
			isArray = false;
			baseComponentType = null;
			isPrimitiveArray = false;
			arrayDimensions = 0;
		}

		isVoid = clazz.equals(Void.class) || clazz.equals(void.class);
		isMatlabType = MatlabType.class.isAssignableFrom(clazz);

		isBuiltinNumeric = clazz.equals(Byte.class) || clazz.equals(byte.class) ||
				clazz.equals(Short.class) || clazz.equals(short.class) ||
				clazz.equals(Integer.class) || clazz.equals(int.class) ||
				clazz.equals(Long.class) || clazz.equals(long.class) ||
				clazz.equals(Float.class) || clazz.equals(float.class) ||
				clazz.equals(Double.class) || clazz.equals(double.class);
	}
}
