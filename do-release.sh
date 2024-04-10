#!/bin/bash

set -euo pipefail

mvn smart-release:prepare
mvn -pl $(cat modules-to-build.txt) -Prelease clean deploy
git push && git push --tags
git restore pom.xml
rm files-to-revert.txt modules-to-build.txt