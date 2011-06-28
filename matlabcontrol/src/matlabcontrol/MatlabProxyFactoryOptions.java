package matlabcontrol;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Options that configure how {@link MatlabProxyFactory} operates. Use the {@link Builder} to create an instance of
 * {@code MatlabProxyFactoryOptions}.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.0.0
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyFactoryOptions
{
    private final String _matlabLocation;
    private final boolean _hidden;
    private final boolean _useRunning;
    private final long _proxyTimeout;
    private final String _logFile;
    private final Integer _jdbPort;
    private final String _licenseFile;
    private final boolean _useSingleCompThread;
    private final int _rmiExternalPort;
    private final int _rmiMatlabPort;
        
    private MatlabProxyFactoryOptions(Builder options)
    {
        _matlabLocation = options._matlabLocation;
        _hidden = options._hidden;
        _useRunning = options._useRunning;
        _proxyTimeout = options._proxyTimeout.get();
        _logFile = options._logFile;
        _jdbPort = options._jdbPort;
        _licenseFile = options._licenseFile;
        _useSingleCompThread = options._useSingleCompThread;
        _rmiExternalPort = options._rmiExternalPort;
        _rmiMatlabPort = options._rmiMatlabPort;
    }

    String getMatlabLocation()
    {
        return _matlabLocation;
    }

    boolean getHidden()
    {
        return _hidden;
    }

    boolean getUseRunningSession()
    {
        return _useRunning;
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
    
    int getRMIExternalJVMPort()
    {
        return _rmiExternalPort;
    }
    
    int getRMIMatlabPort()
    {
        return _rmiMatlabPort;
    }
    
    /**
     * Builds instances of {@link MatlabProxyFactoryOptions}. Any and all of these properties may be left unset, if so
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
        private volatile boolean _hidden = false;
        private volatile boolean _useRunning = true;
        private volatile String _logFile = null;
        private volatile Integer _jdbPort = null;
        private volatile String _licenseFile = null;
        private volatile boolean _useSingleCompThread = false;
        private volatile int _rmiExternalPort = 2100;
        private volatile int _rmiMatlabPort = 2101;
        
        //Assigning to a long is not atomic, so use an AtomicLong to allow thread safety
        private final AtomicLong _proxyTimeout = new AtomicLong(90000L);

        /**
         * Sets the location of the MATLAB executable or script that will launch MATLAB. If the value set cannot be
         * successfully used to launch MATLAB, an exception will be thrown when attempting to create a proxy.
         * <br><br>
         * The absolute path to the MATLAB executable can be determined by running MATLAB. On OS X or Linux, evaluate
         * {@code [matlabroot '/bin/matlab']} in the Command Window. On Windows, evaluate
         * {@code [matlabroot '/bin/matlab.exe']} in the Command Window.
         * <br><br>
         * <strong>Windows</strong><br>
         * The location does not have to be an absolute path so long as the operating system can resolve the path.
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
         * Sets whether MATLAB should appear hidden. By default this property is set to {@code false}. If set to
         * {@code true} then the splash screen will not be shown and:
         * <br><br>
         * <strong>Windows</strong><br>
         * The MATLAB Command Window will appear fully minimized.
         * <br><br>
         * <strong>OS X</strong><br>
         * MATLAB will be entirely hidden.
         * <br><br>
         * <strong>Linux</strong><br>
         * MATLAB will be entirely hidden.
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
         * By default this property is set to {@code true}.
         * <br><br>
         * When this property is {@code true} all options which configure MATLAB such as being hidden or logging are
         * ignored. The only criterion used is whether a session of MATLAB is available for connection. In order for the
         * factory to connect to the session of MATLAB, it must know about the session. This will be the case if any
         * factory launched the session of MATLAB. The factory will only connect to a session that does not currently
         * have a proxy controlling it from outside of MATLAB.
         * 
         * @param useRunning 
         */
        public final Builder setUseRunningSession(boolean useRunning)
        {
            _useRunning = useRunning;
            
            return this;
        }

        /**
         * Sets the amount of time in milliseconds to wait for a proxy to be created when requested via the blocking
         * method {@link MatlabProxyFactory#getProxy()}. By default this property is set to {@code 90000} milliseconds.
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
         * Sets the ports matlabcontrol uses to communicate with MATLAB. By default ports {@code 2100} and {@code 2101}
         * are used. The two port values must be positive and must not be the same. The ports should not be otherwise
         * in use on the system; however, any number of MatlabProxyFactory instances (even those running in completely
         * separate applications) may use the same ports. It is recommended the ports be in the range of {@code 1024} to
         * {@code 49151}, but this is not enforced.
         * 
         * @param port1
         * @param port2
         * @throws IllegalArgumentException if port conditions are not met
         */
        public final Builder setPorts(int port1, int port2)
        {
            if(port1 < 1)
            {
                throw new IllegalArgumentException("port [" + port1 + "] must be positive");
            }
            
            if(port2 < 1)
            {
                throw new IllegalArgumentException("port [" + port2 + "] must be positive");
            }
            
            if(port1 == port2)
            {
                throw new IllegalArgumentException("port values [" + port1 + "] may not be the same");
            }
            
            _rmiExternalPort = port1;
            _rmiMatlabPort = port2;
            
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