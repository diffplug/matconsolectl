# MatConsoleCtl releases

### Version 4.5.0-SNAPSHOT - TBD ([javadoc](http://diffplug.github.io/matconsolectl/javadoc/snapshot/), [jcenter](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/matsim/matconsolectl/))

### Version 4.4.1 - October 16th 2015 ([javadoc](http://diffplug.github.io/matconsolectl/javadoc/4.4.1/), [jcenter](https://bintray.com/diffplug/opensource/matconsolectl/4.4.1/view))

- Corrected the license in the maven metadata.

### Version 4.4.0 - October 16th 2015 ([javadoc](http://diffplug.github.io/matconsolectl/javadoc/4.4.0/), [jcenter](https://bintray.com/diffplug/opensource/matconsolectl/4.4.0/view))

- `MatlabType.MatlabTypeGetter` is now generic.
- Fixed lots of compiler warnings.
- Removed some [unused code](https://github.com/diffplug/matconsolectl/commit/c514188e55880528268dd3314f7347d95d00b7b6), and carefully marked code which [appears unused](https://github.com/diffplug/matconsolectl/commit/60564f2e8a80494b443d7da31c01d2e55c6d72c2) but is actually needed for internal MATLAB scripts.
- Applied DiffPlug's standard formatting and code-quality plugins, FindBugs found several bugs.
- [Fixed bug in ArrayUtils.equals when applied to arrays of long.](https://github.com/diffplug/matconsolectl/commit/088b954551392dc7b24142fd7f1cbcdc6a4005bf)
- [Fixed a serialization bug.](https://github.com/diffplug/matconsolectl/commit/d6bc07adca74f0bb3ae91c1009222eff6b975774)
- Broke up the test suite into `test`, `testMatlabHeadless`, and `testMatlabInteractive`
- Moved the demo code into the main library.  It's a very small demo with no dependencies, makes life easier to manage one jar rather than two.

### Version 4.3.0 - February 23rd 2015

- Added OSGi compatibility.

### Version 4.2.1 - February 22nd 2015

- ThrowableWrapper now initializes the `getMessage()` field with MATLAB's raw error text.

### Version 4.2.0 - February 20th 2015

- Switched to gradle, which makes the jmistub subproject unnecessary.
- Added `CopyPasteCallback` to the set of options for creating a `MatlabProxyFactory`.  The factory sends a chunk of code to the callback, and the user copy-pastes this code into a MATLAB terminal to initiate a connection.
	+ At first, I got a bunch of Serialization errors.  There were a bunch of `Serializable` classes that didn't specify a `serialVersionUID`.  Specifying these seemed to fix the problem.
	+ You can connect over and over this way, and the MATLAB instance stays happy.

## Versions up to 4.1.0 are from the original matlabcontrol project on [Google code page](https://code.google.com/p/matlabcontrol/wiki/VersionHistory).
