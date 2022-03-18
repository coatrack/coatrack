#!/bin/bash

# TODO: All scripts should run with "set -e" and better logging of single steps.
# TODO: Make everything deployable locally via IntelliJ as well.
# TODO: Adapt documentation.

export PROJECT_DIR="${PWD}/.."
export SPRING_COMPONENTS_DIR="${PROJECT_DIR}/spring-boot"
export DOCKER_DIR="${PROJECT_DIR}/docker"
export DOCKER_COMPOSE_DEPLOYMENT_DIR="${DOCKER_DIR}/docker-compose-deployment"
export IMAGE_PUSH_POLICY="suppress-image-pushes"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/stop-and-remove-all-containers-if-existent.sh"
# TODO Make this script executable for its own using pushes (important for Github CI pipeline).
. "${DOCKER_DIR}/build-and-push-all-docker-images.sh"
. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d