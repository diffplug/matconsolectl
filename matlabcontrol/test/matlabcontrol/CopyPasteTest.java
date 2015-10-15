/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import matlabcontrol.MatlabProxyFactory.CopyPasteCallback;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(MatlabRequired.Interactive.class)
public class CopyPasteTest {
	private Runnable runnable;

	@Test
	public void testCopyPaste() throws MatlabConnectionException, MatlabInvocationException {
		MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
		builder.setCopyPasteCallback(new CopyPasteCallback() {
			@Override
			public void copyPaste(String matlabCmdsToConnect) {
				StringBuilder builder = new StringBuilder();
				builder.append("Copy-paste the following lines into a MATLAB:\n");
				String[] pieces = matlabCmdsToConnect.split(";");
				for (String piece : pieces) {
					builder.append("    " + piece.trim() + ";\n");
				}
				builder.append("\nWaiting for you to paste.");
				runnable = MatlabRequired.Interactive.prompt(builder.toString());
			}
		});
		MatlabProxyFactory factory = new MatlabProxyFactory(builder.build());
		MatlabProxy proxy = factory.getProxy();
		try {
			runnable.run();
			proxy.eval("disp('connection established')");
			proxy.setVariable("test", "abc");
			Assert.assertEquals("abc", proxy.getVariable("test"));
		} finally {
			runnable.run();
			proxy.disconnect();
		}
	}
}
