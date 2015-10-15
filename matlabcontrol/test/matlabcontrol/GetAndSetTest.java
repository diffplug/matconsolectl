/*
 * Code licensed under new-style BSD (see LICENSE).
 * All code up to tags/original: Copyright (c) 2013, Joshua Kaplan
 * All code after tags/original: Copyright (c) 2015, DiffPlug
 */
package matlabcontrol;

import matlabcontrol.link.ArrayUtilsTest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
@Category(MatlabRequired.Headless.class)
public class GetAndSetTest {
	private static MatlabProxy proxy;

	@BeforeClass
	public static void createProxy() throws MatlabConnectionException {
		MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
		builder.setUsePreviouslyControlledSession(true);
		MatlabProxyFactory factory = new MatlabProxyFactory(builder.build());
		proxy = factory.getProxy();
	}

	@AfterClass
	public static void exitMatlab() throws MatlabInvocationException {
		if (proxy != null) {
			proxy.disconnect();
		}
	}

	@Before
	public void clear() throws MatlabInvocationException {
		proxy.eval("clear");
	}

	@Test
	public void testSetGet() throws MatlabInvocationException {
		testCaseSetGet(new boolean[]{true});
		testCaseSetGet(new boolean[]{false});

		testCaseSetGet(new double[]{1.5});

		testCaseSetGet("string");
	}

	private void testCaseSetGet(Object set) throws MatlabInvocationException {
		proxy.setVariable("a", set);
		Object get = proxy.getVariable("a");
		ArrayUtilsTest.assertArraysEqual(set, get);
	}
}
