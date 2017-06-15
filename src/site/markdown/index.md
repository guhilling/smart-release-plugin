# The Maven Smart Release Plugin for Git

This plugin is an alternative to the `maven-release-plugin` which was created with the following principles:

* It should be trivial to release modules from a multi-module plugin, and only those modules that have changes should be released
* No commits should be made to a repo during a release
* Maven conventions such as developing against SNAPSHOT versions should be retained
* Git should not need to be installed on the system.

The plugin works with two main ideas:

### Versioning

A software module has two types of versions: the "major version" and the
"minor version". The major version is used for semantic versioning, and may be something like "1", "2", etc.
During development, the version in the pom is the major version with `-SNAPSHOT` appended. During a release, module
version becomes `<major>.<minor>`. However this version is not committed as a change to your pom. Instead the released
versions are recorded in a `.release-info.json` file that is updated during releases. Nothing else will be commited.

This plugin automatically generates minor numbers, starting from 0 and incrementing each time, by looking at previous
releases in the `.release-info.json`.

When performing bugfix releases the bugfix version is automatically created and the version will be
`<major>.<minor>.<bugfix>`, starting with 1 for the bugfix version.

### Handling of the actual release process

Software should be modular. When using this plugin you have to choose how to perform the following steps yourself:

* Building and Deployment of the artifacts. This is usually done simply running `mvn clean deploy`
 (See documentation for details!).
* Pushing changes to your git-master.

## Using the smart-release-plugin

See [Usage](usage.html)

## Prerequisites

The plugin requires Maven 3.0.1 or later and Java 8 or later.