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
