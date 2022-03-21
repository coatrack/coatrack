FROM adoptopenjdk/openjdk11:jre-11.0.14_9-alpine
LABEL maintainer=coatrack.eu

ARG MODULE
ARG PROJECT_VERSION

# File names.
ENV COATRACK_MODULE_JAR="coatrack-${MODULE}-${PROJECT_VERSION}.jar"
ENV COATRACK_GATEWAY_JAR="coatrack-proxy-${PROJECT_VERSION}.jar"

# Project folder directories.
ENV PROJECT_FOLDER_GATEWAY_TARGET_DIR="spring-boot/proxy/target"
ENV PROJECT_FOLDER_MODULE_TARGET_DIR="spring-boot/${MODULE}/target"

# Docker container directories.
ENV CONTAINER_HOME_DIR="/home/coatrack"
ENV CONTAINER_JAR_FILES_DIR="/home/coatrack/jars"
ENV CONTAINER_GATEWAY_CONFIG_FILES_DIR="/home/coatrack/proxy-config-files"

# Copy the module jar to be executed into the docker container.
COPY ${PROJECT_FOLDER_MODULE_TARGET_DIR}/${COATRACK_MODULE_JAR} ${CONTAINER_JAR_FILES_DIR}/${COATRACK_MODULE_JAR}

# Only the config server does not require a gateway jar file.
COPY ${PROJECT_FOLDER_GATEWAY_TARGET_DIR}/${COATRACK_GATEWAY_JAR} ${CONTAINER_JAR_FILES_DIR}/${COATRACK_GATEWAY_JAR}
RUN if [ ${MODULE} == "config-server" ]; then rm ${CONTAINER_JAR_FILES_DIR}/${COATRACK_GATEWAY_JAR}; fi

# Security improvements.
RUN adduser -D -s /bin/false -g coatrack coatrack coatrack
RUN mkdir -p ${CONTAINER_GATEWAY_CONFIG_FILES_DIR} && chown -R coatrack:coatrack ${CONTAINER_HOME_DIR}
USER coatrack
WORKDIR ${CONTAINER_HOME_DIR}
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar ${CONTAINER_JAR_FILES_DIR}/${COATRACK_MODULE_JAR}.jar" ]
