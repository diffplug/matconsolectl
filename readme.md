# matlabcontrol

The real homepage of this project is [here](https://code.google.com/p/matlabcontrol/).  The purpose of this fork is to allow DiffPlug to make a few improvements, which may or may not be of use to the community at large (hopefully they will be!).  Huge thanks to Joshua Kaplan and the other authors of matlabcontrol, it's a great project.

This was forked from 4.1.0.

## Changelog

- **4.2.0**
- Switched to gradle, which makes the jmistub subproject unnecessary.
- Added `CopyPasteCallback` to the set of options for creating a `MatlabProxyFactory`.  The factory sends a chunk of code to the callback, and the user copy-pastes this code into a MATLAB terminal to initiate a connection.
	+ At first, I got a bunch of Serialization errors.  There were a bunch of `Serializable` classes that didn't specify a `serialVersionUID`.  Specifying these seemed to fix the problem.
	+ It seems like you can connect over and over this way, and the MATLAB instance stays happy.
- **4.2.1**
- ThrowableWrapper now initializes the `getMessage()` field with MATLAB's raw error text.

## Quickstart

All the good stuff is in `matlabcontrol`.  Go there and `gradlew build` to build and test, or `gradlew eclipse` to create Eclipse project files, or `gradlew publish` to publish to maven.
