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

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

/**
 * Handles creation of RMI objects, making sure they only operate on localhost.
 *
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalHostRMIHelper
{
    private static final LocalHostRMISocketFactory SOCKET_FACTORY = new LocalHostRMISocketFactory();
    
    public static Registry getRegistry(int port) throws RemoteException
    {
        return LocateRegistry.getRegistry("localhost", port, SOCKET_FACTORY);
    }
    
    public static Registry createRegistry(int port) throws RemoteException
    {
        return LocateRegistry.createRegistry(port, SOCKET_FACTORY, SOCKET_FACTORY);
    }
    
    public static Remote exportObject(Remote object) throws RemoteException
    {
        return UnicastRemoteObject.exportObject(object, 0, SOCKET_FACTORY, SOCKET_FACTORY);
    }
    
    private static class LocalHostRMISocketFactory implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable
    {
        @Override
        public Socket createSocket(String host, int port) throws IOException
        {
            return SocketFactory.getDefault().createSocket(InetAddress.getByName("localhost"), port);
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException
        {
            return ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName("localhost"));
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof LocalHostRMISocketFactory);
        }

        @Override
        public int hashCode()
        {
            return 5;
        }
    }
    
    static class LocalHostRemoteObject extends UnicastRemoteObject
    {
        LocalHostRemoteObject() throws RemoteException
        {
            super(0, SOCKET_FACTORY, SOCKET_FACTORY);
        }
    }
}