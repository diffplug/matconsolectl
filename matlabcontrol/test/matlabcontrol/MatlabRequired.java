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
