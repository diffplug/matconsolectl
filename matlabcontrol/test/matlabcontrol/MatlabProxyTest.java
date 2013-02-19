package matlabcontrol;

import static junit.framework.Assert.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/*
 * Copyright (c) 2013, Joshua Kaplan
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
 *
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyTest
{
    private static MatlabProxy _proxy;
    
    @BeforeClass
    public static void createProxy() throws MatlabConnectionException
    {
        MatlabProxyFactory factory = new MatlabProxyFactory();
        _proxy = factory.getProxy();
    }
    
    @AfterClass
    public static void exitMatlab() throws MatlabInvocationException
    {
        if(_proxy != null)
        {
            _proxy.exit();
        }
    }
    
    @Before
    public void clear() throws MatlabInvocationException
    {
        _proxy.eval("clear");
    }
    
    @Test
    public void testSetVariable() throws MatlabInvocationException
    {
        _proxy.setVariable("a", 5);
    }
    
    @Test
    public void testSetGetVariable() throws MatlabInvocationException
    {
        double expected = 5;
        _proxy.setVariable("a", expected);
        Object result = _proxy.getVariable("a");
        double actual = ((double[]) result)[0];
        assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testEval() throws MatlabInvocationException
    {
        _proxy.eval("disp('Hello World'");
    }
}