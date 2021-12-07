#!/bin/bash

./uninstall.sh
cd ..
mvn package -DskipTests
./build-docker.sh
cd docker-compose-setup
./init.sh
docker-compose up
