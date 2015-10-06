# <img align="left" src="matlabcontrol.png"> matlabcontrol: Control MATLAB from Java

<!---freshmark shields
output = [
	link(shield('Maven artifact', 'mavenCentral', '{{group}}:{{name}}', 'blue'), 'https://bintray.com/{{org}}/opensource/{{name}}/view'),
	link(shield('Latest version', 'latest', '{{stable}}', 'blue'), 'https://github.com/{{org}}/{{name}}/releases/latest'),
	link(shield('Javadoc', 'javadoc', 'OK', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/{{stable}}/'),
	link(shield('License Apache', 'license', 'BSD', 'blue'), 'https://tldrlegal.com/license/bsd-3-clause-license-(revised)'),
	'',
	link(shield('Changelog', 'changelog', '{{version}}', 'brightgreen'), 'CHANGES.md'),
	link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}')
	].join('\n');
-->
[![Maven artifact](https://img.shields.io/badge/mavenCentral-com.diffplug.matsim%3Amatlabcontrol-blue.svg)](https://bintray.com/diffplug/opensource/matlabcontrol/view)
[![Latest version](https://img.shields.io/badge/latest-4.3.0-blue.svg)](https://github.com/diffplug/matlabcontrol/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/matlabcontrol/javadoc/4.3.0/)
[![License Apache](https://img.shields.io/badge/license-BSD-blue.svg)](https://tldrlegal.com/license/bsd-3-clause-license-(revised))

[![Changelog](https://img.shields.io/badge/changelog-4.3.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/matlabcontrol.svg?branch=master)](https://travis-ci.org/diffplug/matlabcontrol)
<!---freshmark /shields -->

matlabcontrol is a Java API that allows calling MATLAB from Java. You can `eval`, `feval`, as well as `get` and `set` variables. Interaction can be performed from either inside MATLAB or outside MATLAB (both by starting a new MATLAB or by connecting to an existing MATLAB).

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
-->

```java
MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
// setup the factory
//    setCopyPasteCallback to connect to an existing MATLAB by copy-pasting a script into
//    setUsePreviouslyControlledSession to start a new MATLAB or connect to a previously started MATLAB
MatlabProxyFactory factory = new MatlabProxyFactory(builder.build());
// get the proxy
MatlabProxy proxy = factory.getProxy();
// do stuff over the proxy
proxy.eval("disp('hello world!)")
proxy.setVariable("a", 5.0);
Object a = proxy.getVariable("a");
double actual = ((double[]) result)[0];
assert(actual == 5.0)
// disconnect
proxy.disconnect();
```

## Compatibility

matlabcontrol works on Win/Mac/Linux, MATLAB R2007b through R2015b, and it will continue to work so long as MATLAB maintains the Java MATLAB Interface.\*

\* On OS X 10.5, R2009a and earlier, you will need to do some hacking.  matlabcontrol requires Java 6, and Apple only released 64-bit Java 6 for OS X 10.5, while MATLAB only released 32-bit MATLAB for R2009a and earlier.  There are unofficial ways to run 32-bit Java 6 on OS X 10.5.

<!---freshmark /javadoc -->

## Acknowledgements

This has been forked from the project originally maintained on the now defunct [Google Code](https://code.google.com/p/matlabcontrol/).

We have made some improvements, and we are happy to work with the community to continue to improve this fantastic library.
However, this fork is in no way associated with or endorsed by any authors of the original project.  We have attempted to make contact with them to make sure
that we are conducting this fork in compliance with their wishes, but have been unable to make contact.

Luckily, the BSD license of the original project allows us to continue development even if we are unable to contact the original authors.
