/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package demo.gui;

import java.awt.Image;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

/**
 * On OS X, sets the demo to have a dock icon and a name in the menu bar. On other operating systems does nothing.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class OSXAppearance {
	static void applyIfApplicable() {
		String osName = System.getProperties().getProperty("os.name");

		if (osName != null && osName.startsWith("Mac OS X")) {
			//Set the System menu bar to be 'matlabcontrol demo'
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "matlabcontrol demo");

			//Set the dock icon using reflection so that no OS X specific classes are referenced - which would be a
			//problem on other platforms
			try {
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
			catch (Exception e) {}
		}
	}
}
