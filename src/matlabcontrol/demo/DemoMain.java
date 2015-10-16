/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

import java.awt.EventQueue;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.WindowConstants;

public class DemoMain {
	public static void main(String[] args) {
		new DemoMain();
	}

	public DemoMain() {
		final int closeOperation = isRunningInsideMATLAB() ? WindowConstants.HIDE_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE;
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				DemoFrame frame = new DemoFrame("matlabcontrol demo - Running Inside MATLAB", null);
				frame.setDefaultCloseOperation(closeOperation);
				frame.setVisible(true);
			}
		});
	}

	private static boolean isRunningInsideMATLAB() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		URL[] urls = ((URLClassLoader) cl).getURLs();
		for (URL url : urls) {
			if (url.toExternalForm().contains("matlab")) {
				return true;
			}
		}
		return false;
	}
}
