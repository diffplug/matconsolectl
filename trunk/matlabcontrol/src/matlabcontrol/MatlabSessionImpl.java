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
 * Implementation of {@link MatlabSession}. Split into interface and implementation to work properly with RMI.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabSessionImpl implements MatlabSession
{   
    private boolean _expectingConnection = false;
    
    @Override
    public synchronized boolean isAvailableForConnection()
    {   
        boolean isAvailable;
        
        if(_expectingConnection)
        {
            isAvailable = false;
        }
        else if(MatlabBroadcaster.areAnyReceiversConnected())
        {
            isAvailable = false;
        }
        //If there is no current connection, then a request to create a proxy will come next
        else
        {
            isAvailable = true;
            _expectingConnection = true;
        }
        
        return isAvailable;
    }

    @Override
    public synchronized void connectFromRMI(String receiverID)
    {
        MatlabConnector.connect(receiverID, true);
    }

    /**
     * Called to notify the broadcaster that an attempt to establish a connection has been completed. Does not
     * communicate if a connection was established successfully, only that the attempt is now done.
     */
    synchronized void connectionAttempted()
    {
        _expectingConnection = false;
    }
}