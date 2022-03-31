#!/bin/bash

export IMAGE_PUSH_POLICY="$1"
source "environment-variables.sh"
cd "${PROJECT_DIR}" || exit 1

# TODO This statement is wrong when this script is called by build-and-deploy-images-locally.sh.
#   TODO The "IMAGE_PUSH_POLICY" is confusing anyhow. I refactor that. Maybe extract script like "build-images",
#     "push-images" etc. or create helper-function.sh  (maybe merge it with environment-variables.sh).
printf "\nBuilding CoatRack module docker images and pushing them into Dockerhub.\n"
echo "  Building jar files of CoatRack modules from source."
mvn clean package -DskipTests

echo "  Building docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  build-single-docker-image "${COATRACK_MODULE}"
  push-single-docker-image "${COATRACK_MODULE}"
done