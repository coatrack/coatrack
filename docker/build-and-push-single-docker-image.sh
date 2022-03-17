#!/bin/bash

MODULE_NAME=${1}
MODULE_VERSION=${2}
MODULE_DIR=${3}

echo "building version $MODULE_VERSION of module $MODULE_NAME in $MODULE_DIR"

docker build -t "coatrack/${MODULE_NAME}:${MODULE_VERSION}" --build-arg MODULE_VERSION="${MODULE_VERSION}" --build-arg MODULE_NAME="${MODULE_NAME}" --build-arg MODULE_DIR="${MODULE_DIR}" .
docker push "coatrack/${MODULE_NAME}:${MODULE_VERSION}"
