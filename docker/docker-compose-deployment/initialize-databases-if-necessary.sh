#!/bin/bash

cd "${DOCKER_COMPOSE_DEPLOYMENT_DIR}" || exit 1
. .env

printf "\nStarting the database initializing procedure.\n"

echo "  Starting database."
docker run --rm -d \
  --name "${DATABASE_CONTAINER_NAME}" \
  -v "${DATABASE_VOLUME}":/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  postgres:"${POSTGRES_VERSION}"

echo "  Wait a fw seconds until database is ready."
sleep 5

echo "  Create databases 'coatrack' and 'ygg_config_server' in PostgreSQL."
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database coatrack;"
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database ygg_config_server;"

echo "  Stopping initialized database."
docker stop "${DATABASE_CONTAINER_NAME}"
