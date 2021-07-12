#!/bin/bash

current_mvn_version=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec);
VERSION=${1:-$current_mvn_version}
echo "building docker images for version $VERSION"

for module in admin proxy "config-server"
do
  bash build-push-docker-image.sh coatrack-$module $VERSION spring-boot/$module
done
