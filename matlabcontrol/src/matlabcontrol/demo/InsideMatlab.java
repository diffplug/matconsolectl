package matlabcontrol.demo;

import java.awt.EventQueue;

import javax.swing.WindowConstants;

/**
 * This class exists solely as a entry point to the demo when running it from inside of MATLAB. By placing it in the
 * default package and giving it the name that it has, it means that once the code is added to MATLAB's Java classpath
 * then the demo can be launched just by typing {@code matlabcontroldemo}. Typing that will cause the constructor of
 * this class to be called.
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class InsideMatlab {
	/**
	 * Launches the demo.
	 */
	public InsideMatlab() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				DemoFrame frame = new DemoFrame("matlabcontrol demo - Running Inside MATLAB", null);
				frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}

	/**
	 * This method will be called by MATLAB to provide the text for the {@code ans} value. By overriding this method
	 * in this manner it will cause this method's return value to be used as a status message:
	 * <pre>
	 * {@code
	 * >> matlabcontroldemo
	 * 
	 * ans =
	 * 
	 * matlabcontrol demo launching...
	 * }
	 * </pre>
	 * 
	 * @return 
	 */
	@Override
	public String toString() {
		return "matlabcontrol demo launching...";
	}
}
