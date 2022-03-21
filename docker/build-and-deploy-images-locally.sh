#!/bin/bash

source "environment-variables.sh"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/stop-containers-and-clean-up-traces-if-existent.sh"
cd "${DOCKER_DIR}" || exit 1
. "${DOCKER_DIR}/build-and-push-images.sh" "suppress-image-pushes"
. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d