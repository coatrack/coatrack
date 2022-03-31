#!/bin/bash

. initialize-databases-if-necessary.sh
INSERT_SAMPLE_DATA_ON_STARTUP=true docker-compose --profile example-gateway up -d