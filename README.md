# <img align="left" src="matconsolectl.png"> MatConsoleCtl: Control MATLAB from Java

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
[![Maven artifact](https://img.shields.io/badge/mavenCentral-com.diffplug.matsim%3Amatconsolectl-blue.svg)](https://bintray.com/diffplug/opensource/matconsolectl/view)
[![Latest version](https://img.shields.io/badge/latest-4.4.1-blue.svg)](https://github.com/diffplug/matconsolectl/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/matconsolectl/javadoc/4.4.1/)
[![License Apache](https://img.shields.io/badge/license-BSD-blue.svg)](https://tldrlegal.com/license/bsd-3-clause-license-(revised))

[![Changelog](https://img.shields.io/badge/changelog-4.4.1-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/matconsolectl.svg?branch=master)](https://travis-ci.org/diffplug/matconsolectl)
<!---freshmark /shields -->

MatConsoleCtl is a Java API that allows calling MATLAB from Java. You can `eval`, `feval`, as well as `get` and `set` variables. Interaction can be performed from either inside MATLAB or outside MATLAB (both by starting a new MATLAB or by connecting to an existing MATLAB).

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
output = prefixDelimiterReplace(output, "version = '", "';", stable);
output = prefixDelimiterReplace(output, 'java -jar matconsolectl-', '.jar', stable);
-->

```java
MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
// setup the factory
//    setCopyPasteCallback() connects to an existing MATLAB by copy-pasting a few lines into the command window
//    setUsePreviouslyControlledSession() starts a new MATLAB or connects to a previously started MATLAB without any user intervention

MatlabProxyFactory factory = new MatlabProxyFactory(builder.build());
// get the proxy
MatlabProxy proxy = factory.getProxy();
// do stuff over the proxy
proxy.eval("disp('hello world!)")
proxy.setVariable("a", 5.0);
Object a = proxy.getVariable("a");
double actual = ((double[]) result)[0];
assert(actual == 5.0)
// disconnect the proxy
proxy.disconnect();
```

Javadoc links for [MatlabProxyFactoryOptions.Builder](http://diffplug.github.io/matconsolectl/javadoc/snapshot/matlabcontrol/MatlabProxyFactoryOptions.Builder.html) and [MatlabProxy](http://diffplug.github.io/matconsolectl/javadoc/snapshot/matlabcontrol/MatlabProxy.html).

Contributions are welcome, see the [contributing guide](CONTRIBUTING.md) for development info.

## Demo

MatConsoleCtl includes a demo GUI.  Below is a script you can use to run the demo inside of MATLAB:

```matlab
version = '4.4.1';
tempdir = 'matconsolectl_demo';

% make a directory to copy the jar into
mkdir tempdir;
% download the jar
URL = ['https://repo1.maven.org/maven2/com/diffplug/matsim/matlabcontrol/' version '/matlabcontrol-' version '.jar'];
filename = [tempdir '/matconsolectl-' version '.jar'];
urlwrite(URL,filename);
% add it to the path
javaaddpath([pwd '\' tempdir]);

% run it
matlabcontrol.demo.DemoMain
```

You can also run the demo outside of MATLAB by downloading the jar, then running `java -jar matconsolectl-4.4.1.jar` at a console.

## Compatibility

MatConsoleCtl works on Win/Mac/Linux, MATLAB R2007b through R2015b, and it will continue to work so long as MATLAB maintains the Java MATLAB Interface.\*

\* On OS X 10.5, R2009a and earlier, you will need to do some hacking.  matlabcontrol requires Java 6, and Apple only released 64-bit Java 6 for OS X 10.5, while MATLAB only released 32-bit MATLAB for R2009a and earlier.  There are unofficial ways to run 32-bit Java 6 on OS X 10.5.

<!---freshmark /javadoc -->

## Acknowledgements

This is forked from the matlabcontrol project originally maintained on the now-defunct [Google Code](https://code.google.com/p/matlabcontrol/).  The name was changed to ensure that we don't infringe the original project's license, but we did not change the package names, so this project is binary compatible with the original matlabcontrol.  This fork is in no way associated with or endorsed by any authors of the original project.

We have fixed some bugs and added some features (see the [changelog](CHANGES.md)), and we will maintain this library into the future.  We're happy to accept pull requests](CONTRIBUTING.md) too!

* Formatted by [spotless](https://github.com/diffplug/spotless), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L70-L90).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/), [as such](https://github.com/diffplug/durian-rx/blob/v1.0/build.gradle?ts=4#L92-L116).
* OSGi metadata generated by JRuyi's [osgibnd-gradle-plugin] (https://github.com/jruyi/osgibnd-gradle-plugin), which leverages Peter Kriens' [bnd](http://www.aqute.biz/Bnd/Bnd).
* Scripts in the `.ci` folder are inspired by [Ben Limmer's work](http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
