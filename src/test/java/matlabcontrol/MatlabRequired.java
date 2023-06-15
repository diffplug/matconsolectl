/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2016, DiffPlug
 */
package matlabcontrol;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.Assert;

public class MatlabRequired {
	/** Marks that this test requires user-in-the-loop interaction with MATLAB. */
	public static class Interactive {
		/** Opens a dialog with the given instructions.  Returns a runnable which closes the dialog. */
		public static Runnable prompt(final String instructions) {
			// figure out the name of the test from the stack
			StackTraceElement[] elements = new Throwable().getStackTrace();
			String testName = "unknown";
			for (int i = 1; i < elements.length; ++i) {
				if (!elements[i].getClassName().startsWith("matlabcontrol.")) {
					StackTraceElement element = elements[i - 1];
					testName = element.getClassName() + "::" + element.getMethodName();
					break;
				}
			}
			final String finalTestName = testName;

			// open a dialog and save it
			final AtomicReference<JFrame> box = new AtomicReference<JFrame>();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JFrame frame = new JFrame();
					box.set(frame);
					frame.setTitle(finalTestName);

					Container contentPane = frame.getContentPane();
					contentPane.setLayout(new BorderLayout());

					JTextArea label = new JTextArea();
					label.setText(instructions);
					contentPane.add(label, BorderLayout.CENTER);

					JButton button = new JButton();
					button.setText("Fail");
					contentPane.add(button, BorderLayout.SOUTH);

					frame.pack();
					frame.setVisible(true);

					button.addActionListener(new java.awt.event.ActionListener() {
						@Override
						public void actionPerformed(java.awt.event.ActionEvent evt) {
							Assert.fail("User clicked fail");
						}
					});
				}
			});

			// return a runnable which will close the dialog
			return new Runnable() {
				@Override
				public void run() {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							JFrame frame = box.get();
							if (frame.isVisible()) {
								frame.setVisible(false);
								;
								frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
							}
						}
					});
				}
			};
		}
	}

	/** Marks that this test requires MATLAB but no user. */
	public static class Headless {}
}
