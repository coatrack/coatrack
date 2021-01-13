#!/bin/bash

export MODULE_NAME=${1}

echo "building version $MVN_VERSION of module $MODULE_NAME"

docker build -t coatrack/$MODULE_NAME:$MVN_VERSION --build-arg MVN_VERSION=$MVN_VERSION --build-arg MODULE_NAME=$MODULE_NAME .
docker push coatrack/$MODULE_NAME:$MVN_VERSION
