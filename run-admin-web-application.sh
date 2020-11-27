#!/bin/bash

# Script to run the CoatRack web application (after it has been built using 'build.sh')

# After startup, please use a web browser to access the application at http://localhost:8080

# Please note: 
# - running CoatRack requires some mail configuration parameters to be set
# - the configuration values in this file are just dummy/fallback values for development builds

COATRACK_JAR_FILE='spring-boot/admin/target/coatrack-admin-*.jar'


if [ -f $COATRACK_JAR_FILE ]; then 

	java -jar -Dygg.mail.sender.user="" -Dygg.mail.sender.password="" -Dygg.mail.server.url="" -Dygg.mail.server.port=0 $COATRACK_JAR_FILE

else
	echo "Error: CoatRack executable file '$COATRACK_JAR_FILE' could not be found."
	echo "Please build CoatRack first before trying to run it, please use the script 'build.sh'."
	exit
fi


