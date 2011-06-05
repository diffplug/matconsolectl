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
 * or outside MATLAB.
 * <br><br>
 * This class is thread-safe.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public final class MatlabProxyFactoryOptions
{
    private String _matlabLocation = null;
    private boolean _hidden = false;
    
    /**
     * Sets the location of the MATLAB executable or script that will launch MATLAB. This does not have to be an
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
     * launch MATLAB.
     * <br><br>
     * <strong>OS X</strong><br>
     * Locations relative to the following will be understood:
     * <ul>
     * <li>The current working directory</li>
     * <li>Directories listed in the {@code PATH} environment variable</li>
     * </ul>
     * On OS X, MATLAB is installed in {@code /Applications/} as an application bundle.
     * <br><br>
     * <strong>Linux</strong><br>
     * Locations relative to the following will be understood:
     * <ul>
     * <li>The current working directory</li>
     * <li>Directories listed in the {@code PATH} environment variable</li>
     * </ul>
     * During the installation process on Linux, MATLAB can create a symbolic link named {@code matlab} that can be used
     * to launch MATLAB.
     * 
     * @param matlabLocation
     */
    public synchronized void setMatlabLocation(String matlabLocation)
    {
        _matlabLocation = matlabLocation;
    }
    
    /**
     * Sets whether MATLAB should appear hidden. If set to {@code true} then the splash screen will not be shown and:
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
     * Constructs an immutable copy of the options.
     * 
     * @return 
     */
    synchronized ImmutableFactoryOptions getImmutableCopy()
    {
        return new ImmutableFactoryOptions(_matlabLocation, _hidden);
    }
    
    /**
     * An immutable version of the factory options.
     */
    static class ImmutableFactoryOptions
    {
        private final String _matlabLocation;
        private final boolean _hidden;
        
        private ImmutableFactoryOptions(String matlabLocation, boolean hidden)
        {
            _matlabLocation = matlabLocation;
            _hidden = hidden;
        }
        
        public String getMatlabLocation()
        {
            return _matlabLocation;
        }
        
        public boolean getHidden()
        {
            return _hidden;
        }
    }
}