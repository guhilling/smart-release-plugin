Changelog
---------

### 4.0 smart-release-plugin

* Split the `release` goal into `prepare` and `cleanup`.
* Change the plugin to be used in a more modular fashion:
    * Use `smart-release:prepare` to prepare the versions.
    * You build and deploy as it's the best way for your project.
    * Push the local commit and tag.
    * Use `smart-release:cleanup` if required.

### 3.8 smart-release-plugin

* Major rewrite to obtain robust bugfix releases.

