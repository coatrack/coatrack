FROM alpine:edge
LABEL maintainer=coatrack.eu

ARG MODULE_VERSION
ARG MODULE_NAME
ARG MODULE_DIR

ENV HOME_DIR="/home/coatrack"
ENV JAR_DIR="/home/coatrack/jars"
ENV CONFIGS_DIR="/home/coatrack/proxy-config-files"
ENV JAVA_OPTS=""
ENV MODULE_NAME=${MODULE_NAME}

RUN apk add --no-cache openjdk11
COPY ${MODULE_DIR}/target/${MODULE_NAME}-${MODULE_VERSION}.jar ${JAR_DIR}/${MODULE_NAME}.jar
COPY spring-boot/proxy/target/coatrack-proxy-${MODULE_VERSION}.jar ${JAR_DIR}/coatrack-proxy-${MODULE_VERSION}.jar
RUN if [ ${MODULE_NAME} != "coatrack-admin" ]; then rm ${JAR_DIR}/coatrack-proxy-${MODULE_VERSION}.jar; fi
RUN adduser -D -s /bin/false -g coatrack coatrack coatrack
RUN mkdir -p ${CONFIGS_DIR} && chown -R coatrack:coatrack ${HOME_DIR}
USER coatrack
WORKDIR ${HOME_DIR}
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar ${JAR_DIR}/$MODULE_NAME.jar" ]
