#!/bin/bash

. "helper-script-building-the-docker-images-and-exporting-environment-variables.sh"

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-coatrack-databases.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d