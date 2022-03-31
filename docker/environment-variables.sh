#!/bin/bash

export PROJECT_DIR="${PWD}/.."
export SPRING_COMPONENTS_DIR="${PROJECT_DIR}/spring-boot"
export DOCKER_DIR="${PROJECT_DIR}/docker"
export DOCKER_COMPOSE_DEPLOYMENT_DIR="${DOCKER_DIR}/docker-compose-deployment"
cd "${PROJECT_DIR}" || exit 1
export COATRACK_VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"

build-single-docker-image () {
  COATRACK_MODULE=${1}
  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${COATRACK_MODULE}:${COATRACK_VERSION}"

  echo "  Building version ${COATRACK_VERSION} of module ${COATRACK_MODULE} in the directory coatrack/${COATRACK_MODULE}."
  docker build -f "${DOCKER_DIR}/Dockerfile" -t "${MODULE_DOCKER_IMAGE_NAME}" --build-arg COATRACK_MODULE="${COATRACK_MODULE}" --build-arg COATRACK_VERSION="${COATRACK_VERSION}" .
}

push-single-docker-image () {
  COATRACK_MODULE=${1}
  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${COATRACK_MODULE}:${COATRACK_VERSION}"

  if [ "${IMAGE_PUSH_POLICY}" != "suppress-image-pushes" ]; then
    echo "  Pushing ${MODULE_DOCKER_IMAGE_NAME} to Dockerhub."
    docker push "${MODULE_DOCKER_IMAGE_NAME}"
  fi
}