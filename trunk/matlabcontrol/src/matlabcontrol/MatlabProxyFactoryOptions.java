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

/**
 * Options that configure how {@link MatlabProxyFactory} operates. Any and all of these properties may be left unset, if
 * so then a default will be used. Whether a given property will be used depends on if the code is running inside MATLAB
 * or outside MATLAB. Currently all properties are used only when running outside MATLAB.
 * <br><br>
 * This class is thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabProxyFactoryOptions
{
    private String _matlabLocation = null;
    private boolean _hidden = false;
    private boolean _useRunning = true;
    private long _proxyTimeout = 90000L;
    
    /**
     * Sets the location of the MATLAB executable or script that will launch MATLAB.
     * <br><br>
     * The absolute path to the MATLAB executable can be determined by running MATLAB. On OS X or Linux, evaluate
     * {@code [matlabroot '/bin/matlab']} in the Command Window. On Windows, evaluate
     * {@code [matlabroot '/bin/matlab.exe']} in the Command Window.
     * <br><br>
     * <strong>Windows</strong><br>
     * The location does not have to be an absolute path so long as the operating system can resolve the path. Locations
     * relative to the following will be understood:
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
     * During the installation process on Linux, MATLAB can create a symbolic link named {@code matlab} that can be used
     * to launch MATLAB. If this property is not set, this symbolic link will be used.
     * 
     * @param matlabLocation
     */
    public synchronized void setMatlabLocation(String matlabLocation)
    {
        _matlabLocation = matlabLocation;
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
    public synchronized void setHidden(boolean hidden)
    {
        _hidden = hidden;
    }
    
    /**
     * Sets whether the factory should attempt to create a proxy that is connected to a running session of MATLAB. By
     * default this property is set to {@code true}.
     * <br><br>
     * In order for the factory to connect to the session of MATLAB, it must know about the session. This will be the
     * case if any factory launched the session of MATLAB or {@link MatlabBroadcaster#broadcast()} was called from
     * inside a session of MATLAB. The factory will only connect to a session that does not currently have a proxy
     * controlling it from outside of MATLAB.
     * 
     * @param useRunning 
     */
    public synchronized void setUseRunningSession(boolean useRunning)
    {
        _useRunning = useRunning;
    }
    
    /**
     * The amount of time in milliseconds to wait for a proxy to be created when requested via the blocking method
     * {@link MatlabProxyFactory#getProxy()}.
     * 
     * @param timeout
     * 
     * @throw IllegalArgumentException if timeout is negative
     */
    public synchronized void setProxyTimeout(long timeout)
    {
        if(timeout < 0L)
        {
            throw new IllegalArgumentException("timeout may not be negative");
        }
        
        _proxyTimeout = timeout;
    }
    
    /**
     * Constructs an immutable copy of the options.
     * 
     * @return 
     */
    synchronized ImmutableFactoryOptions getImmutableCopy()
    {
        return new ImmutableFactoryOptions(this);
    }
    
    /**
     * An immutable version of the factory options.
     */
    static class ImmutableFactoryOptions
    {
        private final String _matlabLocation;
        private final boolean _hidden;
        private final boolean _useRunning;
        private final long _proxyTimeout;
        
        private ImmutableFactoryOptions(MatlabProxyFactoryOptions options)
        {
            _matlabLocation = options._matlabLocation;
            _hidden = options._hidden;
            _useRunning = options._useRunning;
            _proxyTimeout = options._proxyTimeout;
        }
        
        public String getMatlabLocation()
        {
            return _matlabLocation;
        }
        
        public boolean getHidden()
        {
            return _hidden;
        }
        
        public boolean getUseRunningSession()
        {
            return _useRunning;
        }
        
        public long getProxyTimeout()
        {
            return _proxyTimeout;
        }
    }
}