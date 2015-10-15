/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import java.security.Permission;

/**
 * A {@code SecurityManager} that always permits an action to take place. By default a Java application has no
 * {@code SecurityManager} set (although Java applets do). This security manager acts as if there was no security
 * manager in place. matlabcontrol uses Remote Method Invocation (RMI) to communicate with MATLAB when it is used in an
 * application that is not running inside MATLAB. RMI requires a security manager be set in order to allow loading
 * classes that are defined in the other Java Virtual Machine, but not its own. This is for good reason, because
 * allowing arbitrary code to be loaded into an application has the potential for a security exploit. By default RMI
 * allows connections from any external machine unless otherwise configured (or blocked by a firewall). matlabcontrol
 * is configured to prohibit any external connections on the port it is using.
 * <br><br>
 * When matlabcontrol launches a session of MATLAB it installs this security manager so that MATLAB may load classes
 * defined in your application. matlabcontrol does not install this security manager in your program. Installing any
 * security manager will allow your application to receive objects from MATLAB that are of classes defined in MATLAB,
 * but not in your application. Using this security manager is convenient when your application does not need any
 * security beyond the default of having no security manager installed.
 * <br><br>
 * To install this security manager:
 * <pre>
 * {@code
 * System.setSecurityManager(new PermissiveSecurityManager());
 * }
 * </pre>
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class PermissiveSecurityManager extends SecurityManager {
	/**
	 * Always accepts permission request.
	 * 
	 * @param perm 
	 */
	@Override
	public void checkPermission(Permission perm) {}

	/**
	 * Always accepts permission request.
	 * 
	 * @param perm
	 * @param context 
	 */
	@Override
	public void checkPermission(Permission perm, Object context) {}
}
