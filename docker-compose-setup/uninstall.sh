#!/bin/sh

cd "$(dirname "$0")" || exit
docker-compose down
docker volume rm coatrack-admin coatrack-database-volume
docker rmi $(docker images -q coatrack/coatrack-admin)
docker rmi $(docker images -q coatrack/coatrack-config-server)
