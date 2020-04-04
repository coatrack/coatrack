#!/bin/bash
#### IMPORTANT INFORMATION. THIS IS NO READY PRODUCTION SCRIPT, YOU MUST FILL YOUR OWN CONFIG PROPERTIES ####
DATE=$(date +"%Y%m%d")


PGPASSWORD=[PUT YOUR PASSWORD] pg_dump --dbname=postgresql://ygg:$PGPASSWORD@127.0.0.1:5432/ygg > /public/db/coatrack-backup/coatrack-dbexport-$DATE.pgsql

# BitBucket Approach
#cd /public/db/coatrack-backup
#hg commit -m "backup" -A
#hg push

# SFTP approach
cd /public/db

./send-file.sh
