#!/bin/bash

. "environment-variables.sh"

printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
cd "${PROJECT_DIR}" || exit 1
mvn clean package -DskipTests

echo "  Building und pushing docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  build-single-docker-image "${COATRACK_MODULE}"
  push-single-docker-image "${COATRACK_MODULE}"
done