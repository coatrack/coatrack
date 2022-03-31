#!/bin/bash

. "environment-variables.sh"

push-single-docker-image () {
  COATRACK_MODULE=${1}
  MODULE_DOCKER_IMAGE_NAME="coatrack/coatrack-${COATRACK_MODULE}:${COATRACK_VERSION}"

  cd "${PROJECT_DIR}" || exit 1
  echo "  Pushing ${MODULE_DOCKER_IMAGE_NAME} to Dockerhub."
  docker push "${MODULE_DOCKER_IMAGE_NAME}"
}

echo "  Building und pushing docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  cd "${DOCKER_DIR}" || exit 1
  push-single-docker-image "${COATRACK_MODULE}"
done
