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
