# Documentation, download, and usage instructions

**Automatically releases only changed modules of a multi-module maven project.**

Full documentation is available [here as maven site documentation](https://guhilling.github.io/smart-release-plugin/index.html).

The project was started as a fork of Daniel Flowers
[multi-module-maven-release-plugin](https://danielflower.github.io/multi-module-maven-release-plugin/index.html) with
with the goal to create bugfix releases automatically (See [documentation](#creating-a-bugfix-release)).

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
* The actual release creation is up to the user (See [Quick start]).

## Quick Start

Most important: You have to use git as version control system. Support for other VCS is not yet planned.

# Configuration of the plugin

Add the plugin to your pom:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>de.hilling.maven.release</groupId>
                <artifactId>smart-release-plugin</artifactId>
                <version>4.0</version>
            </plugin>
        </plugins>
    </build>
```

## Release

To run the release, use `mvn smart-release:prepare`. This will execute the following steps:

* Sanity checks:
    * Local Repository clean?
    * Version numbers follow the required scheme?
    * No SNAPSHOT-dependencies except the local ones?
* Version management:
    * All changes relative to the latest tag are computed. The latest tag is stored in the `release-info.json` file.
If this file is missing the plugin assumes that all modules are released for the first time.
    * All modules that have changes relative to _their_ latest release (that is stored in `release-info.json`) are
prepared for release by _setting_ their minor number to the next minor release number in their `pom.xml`.
The same is done for all modules
that have transitive dependencies on these modules. The plugin prints out which modules will be released and why.
The `release-info.json` file is commited an tagged. It is not pushed. It is up to you, when you want to push the file
and the corresponding tag.
    * All modules that are _not_ released will not be changed.
* A list of modules to build is stored in `modules-to-build.txt`. The content can be fed directly to the maven `-pl`
(project list) option
* A list of files that need to be reverted later is stored in `files-to-revert.txt`. The `cleanup` goal can be used
to actual revert.
* You do a maven run to build an deploy the modules that have changed. Usually you would run:

```bash
   mvn -pl $(cat modules-to-build.txt) clean deploy
```

* If the deploy-stage was successful you should push the updated `release-info.json` and the corresponding tag:
    * This should actually be performed automatically by jenkins or whatever ci-System you are using to perform your
releases. You are using a ci-System, aren't you?.
    * Of course the details of the git setup are up to you. It is actually one of the advantages of this plugin that it
only does one thing and lets you configure how to handle the rest.

```bash
   git push --all
```
