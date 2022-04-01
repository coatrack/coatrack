#!/bin/bash

export PROJECT_DIR="${PWD}/.."
export SPRING_COMPONENTS_DIR="${PROJECT_DIR}/spring-boot"
export DOCKER_DIR="${PROJECT_DIR}/docker"
export DOCKER_COMPOSE_DEPLOYMENT_DIR="${DOCKER_DIR}/docker-compose-deployment"

cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
. stop-containers-and-clean-up-traces-if-existent.sh

cd "${PROJECT_DIR}" || exit 1
export COATRACK_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
printf "\nBuilding CoatRack module docker images.\n"
echo "  Building jar files of CoatRack modules from source."
mvn clean package -DskipTests jib:dockerBuild

echo "  The CoatRack Admin Image, which does not contain the gateway jar, is being used as base image to create a"
echo "    CoatRack Admin Image which contains this very file."
ID_OF_COATRACK_ADMIN_IMAGE_WITHOUT_GATEWAY_JAR="$(docker images -q coatrack/coatrack-admin)"
docker build -f "${DOCKER_DIR}/dockerfile-injecting-gateway-jar-to-admin-image" -t "coatrack/coatrack-admin:${COATRACK_VERSION}" --build-arg COATRACK_VERSION="${COATRACK_VERSION}" .
# The old CoatRack Admin Image, which does not contain the gateway jar, became redundant and can therefore be deleted.
docker rmi "${ID_OF_COATRACK_ADMIN_IMAGE_WITHOUT_GATEWAY_JAR}"