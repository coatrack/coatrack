#!/bin/bash

. initialize-coatrack-databases.sh
INSERT_SAMPLE_DATA_ON_STARTUP=true docker-compose --profile example-gateway up -d