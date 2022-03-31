#!/bin/bash

build-and-push-single-docker-image () {
  COATRACK_MODULE=${1}
  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${COATRACK_MODULE}:${COATRACK_VERSION}"

  echo "  Building version ${COATRACK_VERSION} of module ${COATRACK_MODULE} in the directory coatrack/${COATRACK_MODULE}."
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "${MODULE_DOCKER_IMAGE_NAME}" --build-arg COATRACK_MODULE="${COATRACK_MODULE}" --build-arg COATRACK_VERSION="${COATRACK_VERSION}" .

  if [ "${IMAGE_PUSH_POLICY}" != "suppress-image-pushes" ]; then
    echo "  Pushing ${MODULE_DOCKER_IMAGE_NAME} to Dockerhub."
    docker push "${MODULE_DOCKER_IMAGE_NAME}"
  fi
}

export IMAGE_PUSH_POLICY="$1"
source "environment-variables.sh"
cd "${PROJECT_DIR}" || exit 1
export COATRACK_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"

# TODO This statement is wrong when this script is called by build-and-deploy-images-locally.sh.
#   TODO The "IMAGE_PUSH_POLICY" is confusing anyhow. I refactor that. Maybe extract script like "build-images",
#     "push-images" etc. or create helper-function.sh  (maybe merge it with environment-variables.sh).
printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
mvn clean package -DskipTests

echo "  Building docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  build-and-push-single-docker-image "${COATRACK_MODULE}"
done