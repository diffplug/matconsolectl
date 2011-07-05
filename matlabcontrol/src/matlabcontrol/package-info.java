/**
 * Contains the core functionality to interact with MATLAB. To interact with MATLAB start by creating a
 * {@link matlabcontrol.MatlabProxyFactory}, and then use it to create a {@link matlabcontrol.MatlabProxy}. The most
 * basic usage pattern of matlabcontrol:
 * <pre>
 * {@code
 * MatlabProxyFactory factory = new MatlabProxyFactory();
 * MatlabProxy proxy = factory.getProxy();
 * ...
 * //Use the proxy as desired 
 * ...
 * proxy.disconnect();
 * }
 * </pre>
 * 
 * @since 3.0.0
 */
package matlabcontrol;