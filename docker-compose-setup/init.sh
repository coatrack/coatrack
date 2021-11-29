#!/bin/sh

cd "$(dirname "$0")/.." || exit

docker run --rm -d -p 5432:5432 \
  --name coatrack-postgres \
  -v coatrack-postgres:/var/lib/postgresql/data \
  -e POSTGRES_PASSWORD=password1234 postgres:9.4.26
sleep 5
docker exec coatrack-postgres psql -U postgres -c "create database coatrack;"
docker exec coatrack-postgres psql -U postgres -c "create database ygg_config_server;"
docker stop coatrack-postgres

cd "docker-compose-setup" || exit
docker-compose up
