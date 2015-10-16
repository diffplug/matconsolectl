/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol.demo;

import java.awt.EventQueue;

import javax.swing.WindowConstants;

/**
 * Launches the demo when running outside MATLAB.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class RemoteMain {
	public static void main(String[] args) {
		final String matlabLocation = (args.length == 1 ? args[0] : null);
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				DemoFrame frame = new DemoFrame("matlabcontrol demo - Running Outside MATLAB", matlabLocation);
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}
}
