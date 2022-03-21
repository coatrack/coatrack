FROM adoptopenjdk/openjdk11:jre-11.0.14_9-alpine
LABEL maintainer=coatrack.eu

ARG MODULE_VERSION
ARG MODULE_NAME
ARG MODULE_DIR

ENV CONTAINER_HOME_DIR="/home/coatrack"
ENV CONTAINER_JAR_FILES_DIR="/home/coatrack/jars"
ENV CONTAINER_GATEWAY_CONFIG_FILES_DIR="/home/coatrack/proxy-config-files"

ENV PROJECT_FOLDER_GATEWAY_TARGET_DIR="spring-boot/proxy/target"
ENV GATEWAY_NAME="coatrack-proxy-${MODULE_VERSION}.jar"
ENV MODULE_NAME=${MODULE_NAME}

# TODO Can I simplify the path using 'PROJECT_FOLDER_GATEWAY_TARGET_DIR'?
COPY ${MODULE_DIR}/target/${MODULE_NAME}-${MODULE_VERSION}.jar ${CONTAINER_JAR_FILES_DIR}/${MODULE_NAME}.jar

# Only the coatrack admin (Web Application) container shall contain the jar file of the gateway/proxy.
COPY ${PROJECT_FOLDER_GATEWAY_TARGET_DIR}/${GATEWAY_NAME} ${CONTAINER_JAR_FILES_DIR}/${GATEWAY_NAME}
RUN if [ ${MODULE_NAME} != "coatrack-admin" ]; then rm ${CONTAINER_JAR_FILES_DIR}/${GATEWAY_NAME}; fi

# Security improvements.
RUN adduser -D -s /bin/false -g coatrack coatrack coatrack
RUN mkdir -p ${CONTAINER_GATEWAY_CONFIG_FILES_DIR} && chown -R coatrack:coatrack ${CONTAINER_HOME_DIR}
USER coatrack
WORKDIR ${CONTAINER_HOME_DIR}
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar ${CONTAINER_JAR_FILES_DIR}/$MODULE_NAME.jar" ]
