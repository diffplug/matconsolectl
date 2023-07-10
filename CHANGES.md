# MatConsoleCtl releases

## [Unreleased] - 2023-07-10
- Support passing custom MATLAB environment & modify build gradle to OSGIfy(add the MANIFEST headers) the generated jar.
- https://github.com/diffplug/matconsolectl/pull/27

## [4.6.0] - 2020-11-12
- Added checks for directories and non-exisiting files in classpath converters: In Configuration.getClassPathAsRMICodebase() and Configuration.getClassPathAsCanonicalPaths(). This prevents unnecessary IOExceptions for invalid classpaths.
- https://github.com/diffplug/matconsolectl/pull/21

## [4.5.0] - 2017-07-18
- Added `Builder.setOutputWriter` and `Builder.setErrorWriter` for capturing `stdout` and `stderr` from MATLAB. ([#20](https://github.com/diffplug/matconsolectl/pull/20))

## [4.4.4] - 2016-12-15
- Yet a better fix to a bug where MatConsoleCtl would not run if there were [spaces in the path](https://github.com/diffplug/matconsolectl/issues/15) to the MatConsoleCtl jar.

## [4.4.3] - 2016-12-15
- Better fix to a bug where MatConsoleCtl would not run if there were [spaces in the path](https://github.com/diffplug/matconsolectl/issues/15) to the MatConsoleCtl jar.

## [4.4.2] - 2015-07-05
- Fixed a bug where MatConsoleCtl would not run if there were [spaces in the path](https://github.com/diffplug/matconsolectl/issues/11) to the MatConsoleCtl jar.

## [4.4.1] - 2015-10-16
- Corrected the license in the maven metadata.

## [4.4.0] - 2015-10-16
- `MatlabType.MatlabTypeGetter` is now generic.
- Fixed lots of compiler warnings.
- Removed some [unused code](https://github.com/diffplug/matconsolectl/commit/c514188e55880528268dd3314f7347d95d00b7b6), and carefully marked code which [appears unused](https://github.com/diffplug/matconsolectl/commit/60564f2e8a80494b443d7da31c01d2e55c6d72c2) but is actually needed for internal MATLAB scripts.
- Applied DiffPlug's standard formatting and code-quality plugins, FindBugs found several bugs.
- [Fixed bug in ArrayUtils.equals when applied to arrays of long.](https://github.com/diffplug/matconsolectl/commit/088b954551392dc7b24142fd7f1cbcdc6a4005bf)
- [Fixed a serialization bug.](https://github.com/diffplug/matconsolectl/commit/d6bc07adca74f0bb3ae91c1009222eff6b975774)
- Broke up the test suite into `test`, `testMatlabHeadless`, and `testMatlabInteractive`
- Moved the demo code into the main library.  It's a very small demo with no dependencies, makes life easier to manage one jar rather than two.

## [4.3.0] - 1015-02-23
- Added OSGi compatibility.

## [4.2.1] - 2015-02-22
- ThrowableWrapper now initializes the `getMessage()` field with MATLAB's raw error text.

## [4.2.0] - 2015-02-20
- Switched to gradle, which makes the jmistub subproject unnecessary.
- Added `CopyPasteCallback` to the set of options for creating a `MatlabProxyFactory`.  The factory sends a chunk of code to the callback, and the user copy-pastes this code into a MATLAB terminal to initiate a connection.
	+ At first, I got a bunch of Serialization errors.  There were a bunch of `Serializable` classes that didn't specify a `serialVersionUID`.  Specifying these seemed to fix the problem.
	+ You can connect over and over this way, and the MATLAB instance stays happy.

## Versions up to 4.1.0 are from the original matlabcontrol project on [Google code page](https://code.google.com/p/matlabcontrol/wiki/VersionHistory).
