package matlabcontrol;

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
public class PermissiveSecurityManager extends SecurityManager
{
    /**
     * Always accepts permission request.
     * 
     * @param perm 
     */
    @Override
    public void checkPermission(Permission perm) { }

    /**
     * Always accepts permission request.
     * 
     * @param perm
     * @param context 
     */
    @Override
    public void checkPermission(Permission perm, Object context) { }
}