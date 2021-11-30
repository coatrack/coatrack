#!/bin/sh

cd "$(dirname "$0")" || exit
export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
cd ..

mvn package -DskipTests
docker run --rm -dit -v "${ADMIN_VOLUME}":/home --name "${ADMIN_CONTAINER_NAME}" ubuntu
docker cp ./spring-boot/proxy/target/coatrack-proxy-"${TAG}".jar "${ADMIN_CONTAINER_NAME}":/home
docker stop "${ADMIN_CONTAINER_NAME}"

docker run --rm -d -p 5432:5432 \
  --name "${DATABASE_CONTAINER_NAME}" \
  -v "${DATABASE_VOLUME}":/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" postgres:"${POSTGRES_VERSION}"
sleep 5
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database coatrack;"
docker exec "${DATABASE_CONTAINER_NAME}" psql -U postgres -c "create database ygg_config_server;"
docker stop "${DATABASE_CONTAINER_NAME}"
