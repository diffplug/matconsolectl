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

import com.mathworks.jmi.MatlabException;

/**
 * A wrapper around {@code com.mathworks.jmi.MatlabException} so that the exception can be sent over RMI without needing
 * the {@code jmi.jar} to be included by the developer, but still prints identically.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class MatlabExceptionWrapper extends Exception
{    
    private static final long serialVersionUID = 1L;
    
    /**
     * The {@code String} representation of the {@code MatlabException} so that this exception can pretend to be a
     * {@code MatlabException}.
     */
    private final String _toString;

    /**
     * Creates a wrapper around {@code innerException} so that when the stack trace is printed it is the same to the
     * developer, but can be easily sent over RMI.
     * 
     * @param innerException
     */
    MatlabExceptionWrapper(MatlabException innerException)
    {
        //Store innerException's toString() value
        _toString = innerException.toString();
        
        //Set this stack trace to that of the innerException
        this.setStackTrace(innerException.getStackTrace());
    }
    
    @Override
    public String toString()
    {
        return _toString;
    }
}