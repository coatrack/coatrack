#!/bin/bash

# Script to run the CoatRack admin web application 

# Optional: pass a Spring profile name as first command line argument 

# After startup, please use a web browser to access the application at http://localhost:8080

# Please note: 
# - running CoatRack requires some mail configuration parameters to be set
# - the configuration values in this file are just dummy/fallback values for development builds

COATRACK_JAR_FILE='./spring-boot/admin/target/coatrack-admin-*.jar'

if [ ! -f $COATRACK_JAR_FILE ]; then 
	echo "--------------------------------------------------------------------------------"
	echo " CoatRack executable file could not be found. Trying to build CoatRack first... "
	echo "--------------------------------------------------------------------------------"
	./build.sh
fi

echo "---------------------------------------"
echo " Running CoatRack Admin application... "
echo "---------------------------------------"

# By default, no Spring profile should be active
VM_ARG_SPRING_PROFILE=""

# If a command line argument was passed, use that as Spring profile name
if [ ! -z $1 ]; then 
	echo " Using Spring profile '$1'"
	echo "---------------------------------------"
	VM_ARG_SPRING_PROFILE="-Dspring.profiles.active=$1"
fi

java -jar $VM_ARG_SPRING_PROFILE -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -Dygg.mail.sender.user=$2 -Dygg.mail.sender.password=$3  $COATRACK_JAR_FILE

