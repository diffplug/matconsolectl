package demo.gui;

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

import java.awt.Image;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;

/**
 * On OS X, sets the demo to have a dock icon and a name in the menu bar. On other operating systems does nothing.
 */
class OSXAppearance
{
    static void applyIfApplicable()
    {
        String osName = System.getProperties().getProperty("os.name");

        if(osName != null && osName.startsWith("Mac OS X"))
        {   
            //Set the System menu bar to be 'matlabcontrol demo'
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "matlabcontrol demo");

            //Set the dock icon using reflection so that no OS X specific classes are referenced - which would be a
            //problem on other platforms
            try
            {
                Image dockIcon = ImageIO.read(OSXAppearance.class.getResource("/demo/gui/icon.png"));

                //The following is equivalent to:
                // com.apple.eawt.Application.getApplication().setDockIconImage(dockIcon);
                Class<?> appClass = Class.forName("com.apple.eawt.Application");
                Method getAppMethod = appClass.getMethod("getApplication");
                Object appInstance = getAppMethod.invoke(null);
                Method dockMethod = appInstance.getClass().getMethod("setDockIconImage", java.awt.Image.class);
                dockMethod.invoke(appInstance, dockIcon);
            }
            //If this does not work, it does not actually matter
            catch(Exception e) { }
        }
    }
}