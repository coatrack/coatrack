#!/usr/bin/expect
#### IMPORTANT INFORMATION. THIS IS NO READY PRODUCTION SCRIPT, YOU MUST FILL YOUR OWN CONFIG PROPERTIES ####
set DATE [exec date +%Y%m%d]

send "This is Coatrack backup of date:  $DATE  \r"
send "Uploading coatrack-backup/coatrack-dbexport-$DATE.pgsql  \r"


spawn sftp u201614-sub1@u201614-sub1.your-storagebox.de:/coatrack-backups 

expect "*id_rsa*"
send "[PUT YOUR PASSWORD]\n" 
expect "*>"
send "put coatrack-backup/coatrack-dbexport-$DATE.pgsql coatrack-backups/ \r"
expect "*>"
send "quit\n"
