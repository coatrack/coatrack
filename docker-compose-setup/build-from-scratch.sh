#!/bin/bash

./uninstall.sh
cd ..
mvn package -DskipTests
./build-docker.sh
cd docker-compose-setup
./install.sh
docker-compose up
