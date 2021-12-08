#!/bin/sh

cd "$(dirname "$0")" || exit
export $(cat .env | grep -v '#' | awk '/=/ {print $1}')
docker-compose down
docker volume rm "${DATABASE_VOLUME}" "${PROXY_CONFIG_FILES_VOLUME}"
docker rmi $(docker images -q coatrack/coatrack-admin)
docker rmi $(docker images -q coatrack/coatrack-config-server)
docker rmi $(docker images -q coatrack/coatrack-proxy)

