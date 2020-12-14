#!/bin/bash

# Script to run the CoatRack admin web application 

# After startup, please use a web browser to access the application at http://localhost:8080

# Please note: 
# - running CoatRack requires some mail configuration parameters to be set
# - the configuration values in this file are just dummy/fallback values for development builds

COATRACK_JAR_FILE='spring-boot/admin/target/coatrack-admin-*.jar'

if [ ! -f $COATRACK_JAR_FILE ]; then 
	echo "--------------------------------------------------------------------------------"
	echo " CoatRack executable file could not be found. Trying to build CoatRack first... "
	echo "--------------------------------------------------------------------------------"
	./build.sh
fi

echo "---------------------------------------"
echo " Running CoatRack Admin application... "
echo "---------------------------------------"
java -jar -Dygg.mail.sender.user="" -Dygg.mail.sender.password="" -Dygg.mail.server.url="" -Dygg.mail.server.port=0 $COATRACK_JAR_FILE

