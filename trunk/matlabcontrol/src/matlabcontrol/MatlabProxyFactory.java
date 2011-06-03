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
 * Creates instances of {@link MatlabProxy}. How these instances will be created depends on whether this code is running
 * inside MATLAB or outside MATLAB.
 */
public class MatlabProxyFactory implements ProxyFactory
{
    private final ProxyFactory _delegateFactory;
    
    /**
     * Constructs the factory with the default options.
     * 
     * @throws MatlabConnectionException 
     */
    public MatlabProxyFactory() throws MatlabConnectionException
    {        
        this(new MatlabProxyFactoryOptions());
    }
    
    /**
     * Constructs the factory with the specified {@code options}. Depending on the whether the factory is running inside
     * MATLAB or outside MATLAB will determine if a given option is used.
     * <br><br>
     * Modifying the options after they have been provided to this constructor will have no effect; they may be modified
     * and used to construct another factory.
     * 
     * @param options
     * @throws MatlabConnectionException 
     */
    public MatlabProxyFactory(MatlabProxyFactoryOptions options) throws MatlabConnectionException
    {
        MatlabProxyFactoryOptions.ImmutableFactoryOptions immutableOptions = options.getImmutableCopy();
                
        if(Configuration.isRunningInsideMatlab())
        {
            _delegateFactory = new LocalMatlabProxyFactory(immutableOptions);
        }
        else
        {
            _delegateFactory = new RemoteMatlabProxyFactory(immutableOptions);
        }
    }

    @Override
    public MatlabProxy getProxy() throws MatlabConnectionException
    {
        return _delegateFactory.getProxy();
    }

    @Override
    public MatlabProxy getProxy(long timeout) throws MatlabConnectionException
    {
        return _delegateFactory.getProxy(timeout);
    }

    @Override
    public String requestProxy() throws MatlabConnectionException
    {
        return _delegateFactory.requestProxy();
    }

    @Override
    public void addConnectionListener(MatlabConnectionListener listener)
    {
        _delegateFactory.addConnectionListener(listener);
    }

    @Override
    public void removeConnectionListener(MatlabConnectionListener listener)
    {
        _delegateFactory.removeConnectionListener(listener);
    }

    @Override
    public void shutdown() throws MatlabConnectionException
    {
        _delegateFactory.shutdown();
    }

    @Override
    public boolean isShutdown()
    {
        return _delegateFactory.isShutdown();
    }
}