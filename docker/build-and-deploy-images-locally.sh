#!/bin/bash

. "environment-variables.sh"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d