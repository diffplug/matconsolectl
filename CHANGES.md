# Durian releases

### Version 4.3.0 - October 7th 2015 ([javadoc](http://diffplug.github.io/matlabcontrol/javadoc/4.3.0/), [jcenter](https://bintray.com/diffplug/opensource/matlabcontrol/4.3.0/view))

- Added `OSGiClassloaderFriendly` to the set of factory options.  When this flag is set to true, RMI is started in a way that doesn't get hosed by OSGi.
- Now available on MavenCentral!

### Version 4.2.1 - February 22nd 2015

- ThrowableWrapper now initializes the `getMessage()` field with MATLAB's raw error text.

### Version 4.2.0 - February 20th 2015

- Switched to gradle, which makes the jmistub subproject unnecessary.
- Added `CopyPasteCallback` to the set of options for creating a `MatlabProxyFactory`.  The factory sends a chunk of code to the callback, and the user copy-pastes this code into a MATLAB terminal to initiate a connection.
	+ At first, I got a bunch of Serialization errors.  There were a bunch of `Serializable` classes that didn't specify a `serialVersionUID`.  Specifying these seemed to fix the problem.
	+ You can connect over and over this way, and the MATLAB instance stays happy.

## Versions up to 4.1.0 are from the original [Google code page](https://code.google.com/p/matlabcontrol/wiki/VersionHistory).
