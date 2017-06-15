Information about releases is kept in `.release-info.json`. Sample from the test projects:

```json
    {
      "tagName": "MULTI_MODULE_RELEASE-2017-04-25-135920",
      "modules": [
        {
          "releaseDate": "2017-04-25T15:59:20.339+02:00[Europe/Berlin]",
          "releaseTag": "MULTI_MODULE_RELEASE-2017-04-25-135920",
          "artifact": {
            "groupId": "de.hilling.maven.release.testprojects.independentversions",
            "artifactId": "independent-versions"
          },
          "version": {
            "majorVersion": 1,
            "minorVersion": 0
          }
        },
        {
          "releaseDate": "2017-04-25T15:59:20.339+02:00[Europe/Berlin]",
          "releaseTag": "MULTI_MODULE_RELEASE-2017-04-25-135920",
          "artifact": {
            "groupId": "de.hilling.maven.release.testprojects.independentversions",
            "artifactId": "core-utils"
          },
          "version": {
            "majorVersion": 2,
            "minorVersion": 0
          }
        },
        {
          "releaseDate": "2017-04-25T15:59:20.339+02:00[Europe/Berlin]",
          "releaseTag": "MULTI_MODULE_RELEASE-2017-04-25-135920",
          "artifact": {
            "groupId": "de.hilling.maven.release.testprojects.independentversions",
            "artifactId": "console-app"
          },
          "version": {
            "majorVersion": 3,
            "minorVersion": 0
          }
        }
      ]
    }
```

You should not have to edit this file manually. In addition you must make sure that you do not merge
`.release-info.json` files from a bugfix branch into your master branch.

