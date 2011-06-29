
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

import demo.gui.DemoFrame;

import java.awt.EventQueue;

import javax.swing.WindowConstants;

/**
 * This class exists solely as a entry point to the demo when running it from inside of MATLAB. By placing it in the
 * default package and giving it the name that it has, it means that once the code is added to MATLAB's Java classpath
 * then the demo can be launched just by typing {@code matlabcontroldemo}. Typing that will cause the constructor of
 * this class to be called.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class matlabcontroldemo
{
    /**
     * Launches the demo.
     */
    public matlabcontroldemo()
    {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                DemoFrame frame = new DemoFrame("matlabcontrol demo - Running Inside MATLAB");
                frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }
    
    /**
     * This method will be called by MATLAB to provide the text for the {@code ans} value. By overriding this method
     * in this manner it will cause this method's return value to be used as a status message:
     * <pre>
     * {@code
     * >> matlabcontroldemo
     * 
     * ans =
     * 
     * matlabcontrol demo launching...
     * }
     * </pre>
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "matlabcontrol demo launching...";
    }
}