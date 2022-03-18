#!/bin/bash

# TODO: Add a mechanism to prevent trying to push in this case. -> Maybe make an extra push script or boolean parameter.
# TODO: All scripts should run with "set -e" and better logging of single steps.
# TODO Replace sh scripts with bash scripts.
# TODO: Make everything deployable locally via IntelliJ as well.
# TODO: Adapt documentation.

export PROJECT_DIR="${PWD}/.."
export SPRING_COMPONENTS_DIR="${PROJECT_DIR}/spring-boot"
export DOCKER_DIR="${PROJECT_DIR}/docker"
export DOCKER_COMPOSE_DEPLOYMENT_DIR="${DOCKER_DIR}/docker-compose-deployment"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/stop-and-remove-all-containers-if-existent.sh"

printf "\nBuilding CoatRack components from source.\n"
cd "${PROJECT_DIR}" || exit 1
# TODO to be uncommented: mvn package -DskipTests

. "${DOCKER_DIR}/build-and-push-all-docker-images.sh"
. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

echo "Starting all services."
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d