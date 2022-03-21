#!/bin/bash

build-and-push-single-docker-image () {
  # TODO Are the arguments still required?
  MODULE_NAME=${1}
  MODULE_VERSION=${2}
  MODULE_DIR=${3}

  echo "  Building version $MODULE_VERSION of module $MODULE_NAME in $MODULE_DIR"
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "coatrack/${MODULE_NAME}:${MODULE_VERSION}" --build-arg MODULE_VERSION="${MODULE_VERSION}" --build-arg MODULE_NAME="${MODULE_NAME}" --build-arg MODULE_DIR="${MODULE_DIR}" .

  if [ "${IMAGE_PUSH_POLICY}" != "suppress-image-pushes" ]; then
    echo "  Pushing coatrack/${MODULE_NAME}:${MODULE_VERSION} to Dockerhub."
    docker push "coatrack/${MODULE_NAME}:${MODULE_VERSION}"
  fi
}

source "environment-variables.sh"

printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
cd "${PROJECT_DIR}" || exit 1
mvn clean package -DskipTests

CURRENT_MVN_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
VERSION=${1:-$CURRENT_MVN_VERSION}
echo "  Building docker images for version ${VERSION}"

for MODULE in "admin" "proxy" "config-server"; do
  build-and-push-single-docker-image "coatrack-${MODULE}" "${VERSION}" "spring-boot/${MODULE}"
done