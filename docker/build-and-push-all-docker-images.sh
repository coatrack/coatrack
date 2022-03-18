#!/bin/bash

build-and-push-single-docker-image () {
  MODULE_NAME=${1}
  MODULE_VERSION=${2}
  MODULE_DIR=${3}

  echo "Building version $MODULE_VERSION of module $MODULE_NAME in $MODULE_DIR"
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "coatrack/${MODULE_NAME}:${MODULE_VERSION}" --build-arg MODULE_VERSION="${MODULE_VERSION}" --build-arg MODULE_NAME="${MODULE_NAME}" --build-arg MODULE_DIR="${MODULE_DIR}" .
  docker push "coatrack/${MODULE_NAME}:${MODULE_VERSION}"
}

# TODO It would make much more sense to apply the maven build command here.

printf "\nBuilding CoatRack component docker images and pushing them into Dockerhub.\n"

cd "${PROJECT_DIR}" || exit 1
CURRENT_MVN_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
VERSION=${1:-$CURRENT_MVN_VERSION}
echo "Building docker images for version ${VERSION}"

for MODULE in "admin" "proxy" "config-server"; do
  build-and-push-single-docker-image "coatrack-${MODULE}" "${VERSION}" "spring-boot/${MODULE}"
done