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

/**
 * This test is necessarily interactive, so it isn't a JUnit test.
 */
public class MatlabProxyFactoryCopyPasteTest {
	public static void main(String[] args) throws MatlabConnectionException, MatlabInvocationException {
		MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
		builder.setCopyPasteCallback(new CopyPasteCallback() {
			@Override
			public void copyPaste(String matlabCmdsToConnect) {
				System.out.println("The following should be copy-pasted into Matlab:");
				System.out.println();
				String[] pieces = matlabCmdsToConnect.split(";");
				for (String piece : pieces) {
					System.out.println(piece.trim() + ";");
				}
				System.out.println();
			}
		});
		MatlabProxyFactory factory = new MatlabProxyFactory(builder.build());
		MatlabProxy proxy = factory.getProxy();
		System.out.println("version=" + proxy.returningEval("version", 1)[0]);
		proxy.exit();
	}
}
