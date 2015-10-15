/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Configures class loading inside of MATLAB such that it will work properly with RMI.
 * <br><br>
 * MATLAB uses {@code com.mathworks.jmi.CustomURLClassLoader} to load classes. Class loaders first ask their parent
 * class loader to attempt to load the class and so on up the chain all the way to the system class loader. (This is by
 * convention, one which {@code CustomURLClassLoader} follows). All classes listed on MATLAB's static class path
 * (defined in the {@code matlabroot/toolbox/local/classpath.txt} file where {@code matlabroot} is the location of
 * MATLAB) will be known to the system class loader. As such, all Java classes listed on the static class path will
 * be loaded by the system class loader. Classes listed on MATLAB's dynamic class path via the {@code javaaddpath}
 * function will be loaded by {@code CustomURLClassLoader}.
 * <br><br>
 * When matlabcontrol is started outside MATLAB and launches a session of MATLAB it adds itself to MATLAB's dynamic
 * classpath. This allows matlabcontrol's code to run inside of MATLAB without needing to find and then modify the
 * classpath.txt file (which also very well might not be desired by the user). When matlabcontrol is started inside
 * MATLAB the user may have placed it on either the static or dynamic class path (the user could do both, but the
 * static class path would be used and the dynamic would be ignored). matlabcontrol cannot initially control which
 * class loader knows about matlabcontrol.
 * <br><br>
 * The reason all of this matters is because of how RMI loads classes. When RMI attempts to load a class it starts by
 * asking the current thread for its class loader (@code Thread#getContextClassLoader}. (If that does not work it can
 * then attempt to load the class from a remote location, but this feature is not used by matlabcontrol and so this
 * step does not occur.) The class loader that is returned by the thread RMI is running on will not be MATLAB's
 * {@code CustomURLClassLoader}. As such, RMI will not operate properly if matlabcontrol was added to the dynamic class
 * path.
 * <br><br>
 * When a class loader is attempting to load a class, it starts with its parent class loader and so on up the chain. By
 * informing the system class loader of the location of matlabcontrol, then RMI's class loader will be able to find the
 * matlabcontrol classes. This can be done via reflection (see {@link #addToSystemClassLoader()}. Once the
 * system class  loader knows about the classes in matlabcontrol it will load the classes. If classes have been loaded
 * by the {@code CustomURLClassLoader} then problems can arise. The classes loaded by the system class loader and those
 * loaded by {@code CustomURLClassLoader} will exist in separate <strong>runtime</strong> packages, meaning they can
 * only access public classes, methods, and fields of one another.
 * <br><br>
 * {@code MatlabClassLoaderHelper} manipulates class loading. If the system class loader knows about matlabcontrol
 * then no action is taken. Otherwise, the system class loader is told about matlabcontrol. When matlabcontrol launches
 * a session of MATLAB this class is called before any other class is loaded, and this class is never used again. As
 * such, if this class is loaded by the {@code CustomURLClassLoader}, it will not be a problem.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabClassLoaderHelper {
	/**
	 * Configures class loading to work properly with RMI inside MATLAB. See the class description for more detail.
	 * Called from within MATLAB when {@link RemoteMatlabProxyFactory} launches a session of MATLAB.
	 * 
	 * @throws MatlabConnectionException 
	 */
	public static void configureClassLoading() throws MatlabConnectionException {
		if (!isOnSystemClassLoader()) {
			addToSystemClassLoader();
		}
	}

	/**
	 * Determines if matlabcontrol is on the system class loader's class path.
	 * 
	 * @return
	 * @throws MatlabConnectionException 
	 */
	private static boolean isOnSystemClassLoader() throws MatlabConnectionException {
		try {
			URI matlabcontrolLocation = MatlabClassLoaderHelper.class.getProtectionDomain().getCodeSource().getLocation().toURI();

			URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			URL[] urls = systemClassLoader.getURLs();

			for (URL url : urls) {
				if (url.toURI().equals(matlabcontrolLocation)) {
					return true;
				}
			}
			return false;
		} catch (ClassCastException e) {
			throw new MatlabConnectionException("Unable to determine if matlabcontrol is on the system class loader's classpath", e);
		} catch (URISyntaxException e) {
			throw new MatlabConnectionException("Unable to determine if matlabcontrol is on the system class loader's classpath", e);
		}
	}

	/**
	 * Adds the location where matlabcontrol exists to the system class loader.
	 * 
	 * @throws MatlabConnectionException 
	 */
	private static void addToSystemClassLoader() throws MatlabConnectionException {
		URL matlabcontrolLocation = MatlabClassLoaderHelper.class.getProtectionDomain().getCodeSource().getLocation();

		try {
			URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

			Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			method.invoke(systemClassLoader, matlabcontrolLocation);
		} catch (Exception e) {
			throw new MatlabConnectionException("Unable to add matlabcontrol to system class loader", e);
		}
	}
}
