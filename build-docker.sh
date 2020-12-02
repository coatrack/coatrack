#!/bin/bash

#
# prerequisites:
#  * create the bxd namespace
#  * create the docker-login secret in the bxd namespace
#  * rabbit container running in rabbit-mq-ha.rabbit
#
#


current_mvn_version=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec);
export MVN_VERSION=${1:-$current_mvn_version}
echo "building docker images for version $MVN_VERSION of bxd-locations"


base=$PWD
cd spring-boot
for module in admin
do
  echo "building $module"
  cd $base/spring-boot/$module
  bash build-push-docker-image.sh
done
