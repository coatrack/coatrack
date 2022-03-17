#!/bin/sh

cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
export $(cat .env | grep -v '#' | awk '/=/ {print $1}')

echo "Container Name: ${DATABASE_CONTAINER_NAME}"

# TODO Open port required?
docker run --rm -d -p 5432:5432 \
  --name "${DATABASE_CONTAINER_NAME}" \
  -v "${DATABASE_VOLUME}":/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  postgres:"${POSTGRES_VERSION}"

sleep 5 # Wait until database is set up.
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database coatrack;"
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database ygg_config_server;"
docker stop "${DATABASE_CONTAINER_NAME}"
