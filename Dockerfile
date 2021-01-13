FROM alpine:edge
MAINTAINER coatrack.eu

RUN apk add --no-cache openjdk11

ARG MVN_VERSION
ARG MODULE_NAME
COPY target/${MODULE_NAME}-${MVN_VERSION}.jar /opt/coatrack/lib/${MODULE_NAME}.jar
ENV JAVA_OPTS=""
ENV MODULE_NAME=${MODULE_NAME}
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/coatrack/lib/$MODULE_NAME.jar" ]
