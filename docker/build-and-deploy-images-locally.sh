#!/bin/bash

THIS_SCRIPTS_DIR=$(dirname "$(readlink -f "$0")")
cd "${THIS_SCRIPTS_DIR}" || exit 1

export IMAGE_PUSH_POLICY="suppress-image-pushes"
source "environment-variables.sh"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/stop-and-remove-all-containers-if-existent.sh"

. "${DOCKER_DIR}/build-and-push-images.sh"
. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d