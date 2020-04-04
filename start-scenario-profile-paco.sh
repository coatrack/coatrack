#scripts

#xterm -e java -Dspring.profiles.active=paco  -jar spring-boot/admin/target/*.jar &
xterm -e java -Dspring.profiles.active=paco -jar spring-boot/proxy/target/*.jar &
xterm -e java -Dspring.profiles.active=paco -jar spring-boot/test-client/target/*.jar &
xterm -e java -Dspring.profiles.active=paco -jar spring-boot/test-service/target/*.jar &
