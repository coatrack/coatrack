#!/bin/bash

. "environment-variables.sh"

cd "${PROJECT_DIR}" || exit 1
mvn clean package -DskipTests

cd "${DOCKER_DIR}" || exit 1
echo "  Building docker images for version ${COATRACK_VERSION}"
for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  build-single-docker-image "${COATRACK_MODULE}"
done

. "${DOCKER_COMPOSE_DEPLOYMENT_DIR}/initialize-databases-if-necessary.sh"

printf "\nStarting all services.\n"
cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
docker-compose up -d