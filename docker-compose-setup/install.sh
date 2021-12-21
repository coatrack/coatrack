#!/bin/sh

cd "$(dirname "$0")" || exit
export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
cd ..

docker run --rm -d -p 5432:5432 \
  --name "${DATABASE_CONTAINER_NAME}" \
  -v "${DATABASE_VOLUME}":/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" postgres:"${POSTGRES_VERSION}"
sleep 5
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database coatrack;"
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database ygg_config_server;"
docker stop "${DATABASE_CONTAINER_NAME}"
