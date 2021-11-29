#!/bin/bash

MODULE_NAME=${1}
MODULE_VERSION=${2}
MODULE_DIR=${3}

echo "building version $MODULE_VERSION of module $MODULE_NAME in $MODULE_DIR"

if [ "$MODULE_NAME" == "coatrack-admin" ]; then
  docker build -t coatrack/$MODULE_NAME:$MODULE_VERSION --build-arg MODULE_VERSION=$MODULE_VERSION --build-arg MODULE_NAME=$MODULE_NAME --build-arg MODULE_DIR=$MODULE_DIR -f ./Dockerfile-Admin .
else
  docker build -t coatrack/$MODULE_NAME:$MODULE_VERSION --build-arg MODULE_VERSION=$MODULE_VERSION --build-arg MODULE_NAME=$MODULE_NAME --build-arg MODULE_DIR=$MODULE_DIR .
fi

docker push coatrack/$MODULE_NAME:$MODULE_VERSION
