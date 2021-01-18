#!/bin/bash


current_mvn_version=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec);
export MVN_VERSION=${1:-$current_mvn_version}
echo "building docker images for version $MVN_VERSION"


base=$PWD
cd spring-boot
for module in admin, config-server
do
  echo "building $module"
  cd $base/spring-boot/$module
  bash build-push-docker-image.sh coatrack-$module
done
