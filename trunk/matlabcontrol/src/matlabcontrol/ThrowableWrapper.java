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
 * A wrapper around any {@link Throwable} so that it  can be sent over RMI without needing the class to be defined in
 * the receiving JVM. The stack trace will print as if it were the original throwable.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class ThrowableWrapper extends Throwable
{    
    private static final long serialVersionUID = 1L;
    
    /**
     * The {@code String} representation of the {@code MatlabException} so that this exception can pretend to be a
     * {@code MatlabException}.
     */
    private final String _toString;

    /**
     * Creates a wrapper around {@code innerThrowable} so that when the stack trace is printed it is the same to the
     * developer, but can be sent over RMI without the throwable being defined in the other JVM.
     * 
     * @param innerThrowable
     */
    ThrowableWrapper(Throwable innerThrowable)
    {
        //Store innerThrowable's toString() value
        _toString = innerThrowable.toString();
        
        //Set this stack trace to that of the innerThrowable
        this.setStackTrace(innerThrowable.getStackTrace());
        
        //Store the cause, wrapping it
        if(innerThrowable.getCause() != null)
        {
            this.initCause(new ThrowableWrapper(innerThrowable.getCause()));
        }
    }
    
    @Override
    public String toString()
    {
        return _toString;
    }
}