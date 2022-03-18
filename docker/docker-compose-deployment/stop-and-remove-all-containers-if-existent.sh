#!/bin/sh

cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
. .env

printf "\nCleaning up by removing currently running containers and volumes if existent.\n"

docker-compose down
docker volume rm "${DATABASE_VOLUME}" "${PROXY_CONFIG_FILES_VOLUME}"

for MODULE in "admin" "proxy" "config-server"; do
  echo "Deleting module coatrack-${MODULE}"
  docker rmi "$(docker images -q coatrack/coatrack-admin)" > /dev/null
done
