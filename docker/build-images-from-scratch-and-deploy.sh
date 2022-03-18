#!/bin/bash

# TODO: All scripts should run with "set -e" and better logging of single steps.
# TODO: Make everything deployable locally via IntelliJ as well.
# TODO: Adapt documentation.

export IMAGE_PUSH_POLICY="suppress-image-pushes"
source "${PWD}/environment-variables.sh"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/stop-and-remove-all-containers-if-existent.sh"

cd "${DOCKER_DIR}" || exit 1
. "${DOCKER_DIR}/build-and-push-all-docker-images.sh"
. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d