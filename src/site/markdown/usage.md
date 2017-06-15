# Usage

## Configuration of the plugin

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

## Creating a bugfix release

To create a bugfix release, follow these steps:

* Create a branch from one of the tags created during a regular release. The `release-info.json` must exist.
* Fix your bugs.
* Create a bugfix release by running `mvn smart-release:prepare -DbugfixRelease=true`
* The same steps as above are run but a new bugfix number will be appended to the latest minor version number.

## Merging changes between branches.

If you try to merge your changes from a bugfix branch into some other branch that is roughly based on master you will
run into the problem that the `release-info.json` files cannot be merged. In fact they are not supposed to be merged:

* Usually you want to merge everything into the "main" branch.
* The `release-info.json` of the current branch is not supposed to change when merging changes from other branches.

At the moment you have to take care of this situation yourself. If you accidently merge the bugfix `.release-info.json`
into your master branch however, the next release will just fail because the version numbers in the bugfix release
are not valid for major releases. So you will be able to fix this accident manually.

## Release-info format

See [release-info]() for details.

### Forcing a release

It is possible to force the release of one or more specified modules, for example if the changes have not been picked up due to some issue.
To do this, use the `forceRelease` parameter. For example:

```bash
	mvn smart-release:prepare -DforceRelease=MyApp
```

In this case the `MyApp` module will be built, even if there where no changes found.

## SSH authentication

Currently, only public key authentication is supported. By default, the plugin reads the private key from `~/.ssh/id_rsa`.
If it's required to use a private key file from another location, you have two opportunities to achieve this:

### Add server section to your Maven settings

This is the preferred way. Firstly, add a server section to your Maven settings 
like this (see <https://maven.apache.org/settings.html#Servers> for further information):

```xml
<settings>
    <servers>
        <server>
            <id>my-server</id>
            <privateKey>/path/to/your/private_key</privateKey>
            <passphrase>optional_passphrase</passphrase> <!-- This is optional -->
        </server>
    </servers>
</settings>
```
	
If your key is password protected, specify the password within element **passphrase**. Tip: do _not confuse_ this with element **password**.

Secondly, specify the `serverId` in the plugin configuration
	
```xml
<plugin>
    ...
    <configuration>
        <serverId>my-server</serverId>
    </configuration>
</plugin>	
```

### Specify private key and optional passphrase in your POM

This is the insecure way to specify your custom private key. Add following properties to your plugin configuration:

```xml
<plugin>
    ...
    <configuration>
        <privateKey>/path/to/your/private_key</privateKey>
        <passphrase>optional_passphrase</passphrase> <!-- This is optional -->
    </configuration>
</plugin>
```

Note: POM configuration has precedence over Maven settings.
	
### Custom known_hosts

Per default, the plugin uses `~/.ssh/known_hosts`. You can override this with following property in
your plugin configuration:

```xml
<plugin>
    ...
    <configuration>
        <knownHosts>/path/to/your/known_hosts</knownHosts>
    </configuration>
</plugin>
```

Note: Maven settings related to `known_hosts` will _not_ be considered by the plugin.

## Contributing

To build and run the tests, you need Java 8 or later and Maven 3 or later. Simply clone and run `mvn install`

Note that the tests run the plugin against a number of sample test projects, located in the `test-projects` folder.
If adding new functionality, or fixing a bug, it is recommended that a sample project be set up so that the scenario
can be tested end-to-end.

See also [CONTRIBUTING.md](CONTRIBUTING.md) for information on deploying to Nexus and releasing the plugin.
