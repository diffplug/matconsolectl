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

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Options that configure how a factory operates. Use a {@link Builder} to create an instance of this class.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @see MatlabProxyFactory#MatlabProxyFactory(matlabcontrol.MatlabProxyFactoryOptions) 
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyFactoryOptions
{
    private final String _matlabLocation;
    private final File _startingDirectory;
    private final boolean _hidden;
    private final boolean _usePreviouslyControlled;
    private final long _proxyTimeout;
    private final String _logFile;
    private final Integer _jdbPort;
    private final String _licenseFile;
    private final boolean _useSingleCompThread;
    private final int _port;
        
    private MatlabProxyFactoryOptions(Builder options)
    {
        _matlabLocation = options._matlabLocation;
        _startingDirectory = options._startingDirectory;
        _hidden = options._hidden;
        _usePreviouslyControlled = options._usePreviouslyControlled;
        _proxyTimeout = options._proxyTimeout.get();
        _logFile = options._logFile;
        _jdbPort = options._jdbPort;
        _licenseFile = options._licenseFile;
        _useSingleCompThread = options._useSingleCompThread;
        _port = options._port;
    }

    String getMatlabLocation()
    {
        return _matlabLocation;
    }
    
    File getStartingDirectory()
    {
        return _startingDirectory;
    }

    boolean getHidden()
    {
        return _hidden;
    }

    boolean getUsePreviouslyControlledSession()
    {
        return _usePreviouslyControlled;
    }

    long getProxyTimeout()
    {
        return _proxyTimeout;
    }

    String getLogFile()
    {
        return _logFile;
    }

    Integer getJavaDebugger()
    {
        return _jdbPort;
    }

    String getLicenseFile()
    {
        return _licenseFile;
    }

    boolean getUseSingleComputationalThread()
    {
        return _useSingleCompThread;
    }
    
    int getPort()
    {
        return _port;
    }
    
    /**
     * Creates instances of {@link MatlabProxyFactoryOptions}. Any and all of these properties may be left unset, if so
     * then a default will be used. Depending on how the factory operates, not all properties may be used. Currently all
     * properties are used only when running outside MATLAB, but future releases may add additional options.
     * <br><br>
     * Calls on this class may be chained together to easily create factory options. Example usage:
     * <pre>
     * {@code
     * MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
     *                                         .setHidden(true)
     *                                         .setProxyTimeout(30000L)
     *                                         .build();
     * }
     * </pre>
     * This class is unconditionally thread-safe.
     * 
     * @since 4.0.0
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static class Builder
    {
        private volatile String _matlabLocation = null;
        private volatile File _startingDirectory = null;
        private volatile boolean _hidden = false;
        private volatile boolean _usePreviouslyControlled = false;
        private volatile String _logFile = null;
        private volatile Integer _jdbPort = null;
        private volatile String _licenseFile = null;
        private volatile boolean _useSingleCompThread = false;
        private volatile int _port = 2100;
        
        //Assigning to a long is not atomic, so use an AtomicLong so that a thread always sees an intended value
        private final AtomicLong _proxyTimeout = new AtomicLong(180000L);

        /**
         * Sets the location of the MATLAB executable or script that will launch MATLAB. If the value set cannot be
         * successfully used to launch MATLAB, an exception will be thrown when attempting to create a proxy.
         * <br><br>
         * The absolute path to the MATLAB executable can be determined by running MATLAB. On OS X or Linux, evaluate
         * {@code [matlabroot '/bin/matlab']} in the Command Window. On Windows, evaluate
         * {@code [matlabroot '/bin/matlab.exe']} in the Command Window. The location provided does not have to be an
         * absolute path so long as the operating system can resolve the path.
         * <br><br>
         * <strong>Windows</strong><br>
         * Locations relative to the following will be understood:
         * <ul>
         * <li>The current working directory</li>
         * <li>The {@code Windows} directory only (no subdirectories are searched)</li>
         * <li>The {@code Windows\System32} directory</li>
         * <li>Directories listed in the {@code PATH} environment variable</li>
         * <li>App Paths defined in the registry with key
         *     {@code HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths}</li>
         * </ul>
         * By default on Windows, MATLAB places an App Path entry in the registry so that {@code matlab} can be used to
         * launch MATLAB. If this property is not set, this App Path entry will be used.
         * <br><br>
         * <strong>OS X</strong><br>
         * Locations relative to the following will be understood:
         * <ul>
         * <li>The current working directory</li>
         * <li>Directories listed in the {@code PATH} environment variable</li>
         * </ul>
         * On OS X, MATLAB is installed in {@code /Applications/} as an application bundle. If this property is not set,
         * the executable inside of the application bundle will be used.
         * <br><br>
         * <strong>Linux</strong><br>
         * Locations relative to the following will be understood:
         * <ul>
         * <li>The current working directory</li>
         * <li>Directories listed in the {@code PATH} environment variable</li>
         * </ul>
         * During the installation process on Linux, MATLAB can create a symbolic link named {@code matlab} that can be
         * used to launch MATLAB. If this property is not set, this symbolic link will be used.
         * 
         * @param matlabLocation
         */
        public final Builder setMatlabLocation(String matlabLocation)
        {
            _matlabLocation = matlabLocation;
            
            return this;
        }
        
        /**
         * Sets the starting directory for MATLAB.
         * 
         * @param dir
         * @throws NullPointerException if {@code dir} is {@code null}
         * @throws IllegalArgumentException if {@code dir} does not exist or is not a directory 
         */
        public final Builder setMatlabStartingDirectory(File dir)
        {
            if(dir == null)
            {
                throw new NullPointerException("dir may not be null");
            }
            
            if(!dir.exists())
            {
                throw new IllegalArgumentException("dir specifies a directory that does not exist");
            }
            
            if(!dir.isDirectory())
            {
                throw new IllegalArgumentException("dir does not specify a directory");
            }
            
            _startingDirectory = dir;
            
            return this;
        }

        /**
         * Sets whether MATLAB should appear hidden. By default this property is set to {@code false}. If set to
         * {@code true} then the splash screen will not be shown and:
         * <br><br>
         * <strong>Windows</strong><br>
         * The MATLAB Command Window will appear fully minimized.
         * <br><br>
         * <strong>OS X</strong><br>
         * MATLAB will be entirely hidden. The MATLAB session will terminate when the Java application terminates.
         * <br><br>
         * <strong>Linux</strong><br>
         * MATLAB will be entirely hidden. The MATLAB session will terminate when the Java application terminates.
         * 
         * @param hidden 
         */
        public final Builder setHidden(boolean hidden)
        {
            _hidden = hidden;
            
            return this;
        }

        /**
         * Sets whether to have MATLAB log any output to the MATLAB Command Window (including crash reports) to the
         * file specified by {@code logFile}. The validity of {@code logFile} is not checked by matlabcontrol. By
         * default output is not logged.
         * 
         * @param logFile 
         */
        public final Builder setLogFile(String logFile)
        {
            _logFile = logFile;
            
            return this;
        }

        /**
         * Sets whether to enable use of the Java debugger on the MATLAB session using port {@code portnumber}. The
         * {@code portnumber} may be in the range {@code 0-65535} so long as it is not reserved or otherwise in use. By
         * default the Java debugger is not enabled.
         * 
         * @param portnumber 
         * @throws IllegalArgumentException if {@code portnumber} is not in the range {@code 0-65535}
         */
        public final Builder setJavaDebugger(int portnumber)
        {
            if(portnumber < 0 || portnumber > 65535)
            {
                throw new IllegalArgumentException("port number [" + portnumber + "] must be in the range 0-65535");
            }

            _jdbPort = portnumber;
            
            return this;
        }

        /**
         * Sets the license file used by MATLAB. By default no license file is specified. On Linux and OS X
         * {@code licenseFile} may have the form {@code port@host} or a colon-separated list of license filenames. On
         * Windows {@code licenseFile} may have the form {@code port@host}. Setting this option causes the
         * {@code LM_LICENSE_FILE} and {@code MLM_LICENSE_FILE} environment variables to be ignored. The validity of
         * {@code licenseFile} is not checked by matlabcontrol. 
         * 
         * @param licenseFile 
         */
        public final Builder setLicenseFile(String licenseFile)
        {
            _licenseFile = licenseFile;
            
            return this;
        }

        /**
         * Sets whether the factory should attempt to create a proxy that is connected to a running session of MATLAB.
         * By default this property is set to {@code false}.
         * <br><br>
         * When this property is {@code true} all options which configure MATLAB such as being hidden or logging are
         * ignored. The only criterion used is whether a session of MATLAB is available for connection. In order for the
         * factory to connect to the session of MATLAB, it must know about the session. This will be the case if a
         * factory started the session of MATLAB and that factory was configured to use the same port as specified by
         * {@link #setPort(int)} (or both are using the default port). The factory will only connect to a session that
         * does not currently have a proxy controlling it from outside of MATLAB.
         * <br><br>
         * To determine if the proxy created is connected to an existing session of MATLAB call
         * {@link MatlabProxy#isExistingSession()}. You may wish to clear MATLAB's environment using {@code clear}.
         * Doing so will not in anyway interfere with matlabcontrol (including executing {@code clear java}).
         * <br><br>
         * If a running session of MATLAB previously loaded classes defined in the controlling application, issues
         * can arise. If your application does send to MATLAB or retrieve from MATLAB custom
         * {@link java.io.Serializable} or {@link java.rmi.Remote} classes then these issues do not apply.
         * <br><br>
         * MATLAB sessions launched by matlabcontrol are able to load classes defined in the controlling application.
         * When an existing session of MATLAB is connected to by a newly controlling application it will now be able to
         * load classes defined by the newly controlling application but not the previous one. Several problems may
         * arise due to this behavior. If an attempt is made to use a class defined in a previously controlling session
         * that was not loaded while the application was controlling MATLAB then it will fail with a
         * {@code ClassNotFoundException} if it is not also defined in the newly controlling application. If the class
         * is defined it will fail to load it if the serializable definition is not compatible. A similar issue is if
         * the newly controlling application attempts to send to MATLAB an instance of a class that was also defined by
         * the previously controlling application but the serializable definition is not compatible. These above issues
         * can easily be encountered when developing an application while changing {@code Serializable} or
         * {@code Remote} classes and using the same session of MATLAB repeatedly. This will particularly be the case if
         * the classes do not define a {@code serialVersionUID}. If multiple instances of the same application do not
         * vary in their definition of {@code Serializable} and {@code Remote} classes then connecting to a previously
         * controlled session of MATLAB will not cause any issues in this regard.
         * 
         * @param usePreviouslyControlled 
         */
        public final Builder setUsePreviouslyControlledSession(boolean usePreviouslyControlled)
        {
            _usePreviouslyControlled = usePreviouslyControlled;
            
            return this;
        }

        /**
         * Sets the amount of time in milliseconds to wait for a proxy to be created when requested via the blocking
         * method {@link MatlabProxyFactory#getProxy()}. By default this property is set to {@code 180000} milliseconds.
         * 
         * @param timeout
         * 
         * @throws IllegalArgumentException if timeout is negative
         */
        public final Builder setProxyTimeout(long timeout)
        {
            if(timeout < 0L)
            {
                throw new IllegalArgumentException("timeout [" + timeout + "] may not be negative");
            }
            
            _proxyTimeout.set(timeout);
            
            return this;
        }

        /**
         * Sets whether to limit MATLAB to a single computational thread. By default this property is set to
         * {@code false}.
         * 
         * @param useSingleCompThread 
         */
        public final Builder setUseSingleComputationalThread(boolean useSingleCompThread)
        {
            _useSingleCompThread = useSingleCompThread;
            
            return this;
        }
        
        /**
         * Sets the port matlabcontrol uses to communicate with MATLAB. By default port {@code 2100} is used. The port
         * value may not be negative. It is recommended to be in the range of {@code 1024} to {@code 49151}, but this
         * range is not enforced. The port should be otherwise unused; however, any number of {@link MatlabProxyFactory}
         * instances (even those running in completely separate applications) may use the same port. A 
         * {@code MatlabProxyFactory} will only be able to connect to a previously controlled running session that was
         * started by a factory using the same {@code port}.
         * 
         * @param port
         * @throws IllegalArgumentException if port is negative
         */
        public final Builder setPort(int port)
        {
            if(port < 0)
            {
                throw new IllegalArgumentException("port [" + port + "] may not be negative");
            }
            
            _port = port;
            
            return this;
        }
        
        /**
         * Builds a {@code MatlabProxyFactoryOptions} instance.
         * 
         * @return factory options
         */
        public final MatlabProxyFactoryOptions build()
        {
            return new MatlabProxyFactoryOptions(this);
        }
    }
}