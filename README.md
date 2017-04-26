# Documentation, download, and usage instructions

**Automatically releases only changed modules of a multi-module maven project.**

The project was started as a fork of Daniel Flowers [multi-module-maven-release-plugin](http://danielflower.github.io/multi-module-maven-release-plugin/index.html)
to add some features, mostly creating bugfix releases.

## Development

[![Build Status](https://travis-ci.org/guhilling/smart-release-plugin.svg?branch=master&maxAge=60)](https://travis-ci.org/guhilling/smart-release-plugin)
[![Coverage Status](https://coveralls.io/repos/github/guhilling/smart-release-plugin/badge.svg?branch=master&maxAge=60)](https://coveralls.io/github/guhilling/smart-release-plugin?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/de.hilling.maven.release/smart-release-plugin.svg?maxAge=60)](http://search.maven.org/#search|gav|1|g:"de.hilling.maven.release"%20AND%20a:"smart-release-plugin")

## Features

* Automatically releases only changed modules of a multi-module maven project.
    * Dependencies are automatically resolved transitively.
    * Version numbers must follow format <Major>-SNAPSHOT.
    * Minor and bugfix numbers are chosen automatically.
    * Regular releases increase the minor number in the resulting artifacts.
    * Bugfix releases increase the bugfix number relative to the latest regular release.
* Allows to create bugfix-releases in bugfix branches.
    * Use flag -DperformBugfixRelease to trigger bugfix.
* Tracks the released versions robust and efficient in release-files.
* Builds the modules in two steps:
    * Tests are only run once.
    * In the second step the local changes are pushed an the artifacts are deployed.
    * The steps can be configured.

## Quick Start

Most important: You have to use git as version control system. Support for other VCS is no yet planned.

### Project Setup

Your project modules must use version numbers following the scheme `<Major>-SNAPSHOT`.
This is necessary because the release will resolve the next minor number to be released from the tag history and the
release file `.release-info.json` that is stored in the root of the project after the first release.

### Plugin Configuration

A sample configuration looks like this:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>de.hilling.maven.release</groupId>
                <artifactId>smart-release-plugin</artifactId>
                <version>3.8</version>
                <configuration>
                    <testGoals>
                        <testGoal>clean</testGoal>
                        <testGoal>install</testGoal>
                        <testGoal>-Dmaven.javadoc.skip=true</testGoal>
                    </testGoals>
                    <releaseGoals>
                        <releaseGoal>deploy</releaseGoal>
                    </releaseGoals>
                    <releaseProfiles>
                        <releaseProfile>release</releaseProfile>
                    </releaseProfiles>
                </configuration>
            </plugin>
        </plugins>
    </build>

```

### Releasing the project

To run the release, use `mvn smart-release-plugin:release`. This will execute the following steps:

* The usual sanity checks:
  * Local Repository clean?
  * No SNAPSHOT dependencies on non-local artifacts?
  * Version numbers follow the required scheme?
* All changes relative to the latest tag are computed. The latest tag is stored in the `release-info.json` file.
If this file is missing the plugin assumes that all modules are released for the first time.
* All modules that have changes relative to _their_ latest release (that is stored in `release-info.json`) are 
prepared for release by _setting_ their minor number to the next minor release number in their `pom.xml`.
The same is done for all modules
that have transitive dependencies on these modules. The plugin prints out which modules will be released and why.
* All modules that are _not_ released will not be changed. Only the modules to be released will be built in the 
following steps using maven's `--projects` option. This has two advantages:
  * Faster builds.
  * During releases the artifacts for modules that are not going to be released are actually taken from the repository.
* The first run is performed. By default this means running `mvn -Prelease clean install --projects <modules to be released>`.
The goals can be configured using the `testGoals` property.
* The new `release-info.json` is tagged and pushed to the origin. The changed poms are not commited or pushed. The tag
will be named `MULTI_MODULE_RELEASE-<YYYY-MM-dd-HHmmss>`. The time used is UTC. The `release-info.json` will look
like:
```json
{
  "tagName": "MULTI_MODULE_RELEASE-2017-04-12-150927",
  "modules": [
    {
      "releaseDate": "2017-04-12T17:09:27.452+02:00[Europe/Berlin]",
      "releaseTag": "MULTI_MODULE_RELEASE-2017-04-12-150927",
      "artifact": {
        "groupId": "de.hilling.maven.release",
        "artifactId": "smart-release-plugin"
      },
      "version": {
        "majorVersion": 3,
        "minorVersion": 8
      }
    }
  ]
}
```
* The second run is performed. By default this means running `mvn -Prelease deploy --projects <modules to be released>`.
* All changes to the poms are reverted. 

### Creating a bugfix release

To create a bugfix release, follow these steps:

* Create a branch from one of the tags created during a regular release. The `release-info.json` must exist.
* Fix your bugs.
* Create a bugfix release by running `mvn smart-release-plugin:release -DbugfixRelease=true`
* The same steps as above are run but a new bugfix number will be appended to the latest minor version number.

### Merging changes between branches.

If you try to merge your changes from a bugfix branch into some other branch that is roughly based on master you will
run into the problem that the `release-info.json` files cannot be merged. In fact they are not supposed to be merged:

* Usually you want to merge everything into the "main" branch.
* The `release-info.json` of the current branch is not supposed to change when merging changes from other branches.

At the moment you have to take care of this situation yourself. If you accidently merge the bugfix `.release-info.json`
into your master branch however, the next release will just fail because the version numbers in the bugfix release
are not valid for major releases. So you will be able to fix this accident manually.

## Contributing

To build and run the tests, you need Java 8 or later and Maven 3 or later. Simply clone and run `mvn install`

Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.

See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.

##Stability stuff

* Figure out if things like MVN_OPTIONS and other JVM options need to be passed during release
