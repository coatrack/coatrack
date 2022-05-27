FROM coatrack/coatrack-admin:2.0.0-SNAPSHOT
LABEL maintainer=coatrack.eu

ARG COATRACK_VERSION
ENV COATRACK_GATEWAY_JAR="coatrack-proxy-${COATRACK_VERSION}.jar"
COPY "spring-boot/proxy/target/${COATRACK_GATEWAY_JAR}" "/app/${COATRACK_GATEWAY_JAR}"
