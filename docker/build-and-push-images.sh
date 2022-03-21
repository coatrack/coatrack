#!/bin/bash

build-and-push-single-docker-image () {
  MODULE=${1}
  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${MODULE}:${PROJECT_VERSION}"

  echo "  Building version ${PROJECT_VERSION} of module ${MODULE} in the directory coatrack/${MODULE}."
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "${MODULE_DOCKER_IMAGE_NAME}" --build-arg MODULE="${MODULE}" --build-arg PROJECT_VERSION="${PROJECT_VERSION}" .

  if [ "${IMAGE_PUSH_POLICY}" != "suppress-image-pushes" ]; then
    echo "  Pushing ${MODULE_DOCKER_IMAGE_NAME} to Dockerhub."
    docker push "${MODULE_DOCKER_IMAGE_NAME}"
  fi
}

export IMAGE_PUSH_POLICY="$1"
source "environment-variables.sh"
cd "${PROJECT_DIR}" || exit 1
export PROJECT_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"

printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
mvn clean package -DskipTests

echo "  Building docker images for version ${PROJECT_VERSION}"
for MODULE in "admin" "proxy" "config-server"; do
  build-and-push-single-docker-image "${MODULE}"
done