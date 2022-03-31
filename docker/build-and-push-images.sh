#!/bin/bash

. "environment-variables.sh"

echo "  Building und pushing docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  cd "${DOCKER_DIR}" || exit 1
  push-single-docker-image "${COATRACK_MODULE}"
done