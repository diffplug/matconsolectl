package matlabcontrol;


import java.security.Permission;

/**
 * A {@link SecurityManager} that always allows permits an action to take place. By default a Java application has no
 * {@code SecurityManager} set (but Java applets do). This {@code SecurityManager} acts as if there was no security
 * manager in place. However, Remote Method Invocation (RMI) requires a {@code SecurityManager} be set in order to allow
 * loading classes that are defined in the other Java Virtual Machine, but not its own. This is for good reason, because
 * allowing arbitrary code to be loaded into an application has the potential for a security exploit. If the machine has
 * the ports used by matlabcontrol exposed to the Internet, this {@code SecurityManager} should not be used.
 * <br><br>
 * TODO: Document this more
 */
public class PermissiveSecurityManager extends SecurityManager
{
    @Override
    public void checkPermission(Permission perm) { }

    @Override
    public void checkPermission(Permission perm, Object context) { }
}