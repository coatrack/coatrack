#!/bin/bash

build-and-push-single-docker-image () {
  MODULE=${1}
  MODULE_VERSION=${2}

  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${MODULE}:${MODULE_VERSION}"

  echo "  Building version ${MODULE_VERSION} of module ${MODULE} in the directory coatrack/${MODULE}."
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "${MODULE_DOCKER_IMAGE_NAME}" --build-arg MODULE="${MODULE}" --build-arg MODULE_VERSION="${MODULE_VERSION}" .

  if [ "${IMAGE_PUSH_POLICY}" != "suppress-image-pushes" ]; then
    echo "  Pushing ${MODULE_DOCKER_IMAGE_NAME} to Dockerhub."
    docker push "${MODULE_DOCKER_IMAGE_NAME}"
  fi
}

source "environment-variables.sh"

printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
cd "${PROJECT_DIR}" || exit 1
mvn clean package -DskipTests

CURRENT_MVN_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
MODULE_VERSION=${1:-$CURRENT_MVN_VERSION}
echo "  Building docker images for version ${VERSION}"

for MODULE in "admin" "proxy" "config-server"; do
  build-and-push-single-docker-image "${MODULE}" "${MODULE_VERSION}"
done