#!/bin/bash

cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
. .env

printf "\nCleaning up by removing currently running containers and volumes if existent.\n"

docker-compose down
docker volume rm "${DATABASE_VOLUME}" "${GATEWAY_CONFIG_FILES_VOLUME}"

for COATRACK_MODULE in "admin" "proxy" "config-server"; do
  echo "  Deleting module coatrack-${COATRACK_MODULE}"
  docker rmi "coatrack/coatrack-${COATRACK_MODULE}:${COATRACK_VERSION}" > /dev/null
done
