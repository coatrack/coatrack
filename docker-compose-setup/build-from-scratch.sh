#!/bin/bash

#Pulls latest tag from dockerhub repository. TODO Uncomment as soon as building local docker images is no longer required. Maybe "latest" could also be sufficient.
#TAG=$(wget -q https://registry.hub.docker.com/v1/repositories/coatrack/coatrack-admin/tags -O -  | sed -e 's/[][]//g' -e 's/"//g' -e 's/ //g' | tr '}' '\n'  | awk -F: '{print $3}' | tail -1)
export TAG=2.0.0-SNAPSHOT

./uninstall.sh
cd ..
mvn package -DskipTests
./build-docker.sh
cd docker-compose-setup
./install.sh
docker-compose up
