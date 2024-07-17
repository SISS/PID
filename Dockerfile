ARG BUILD_REVISION="1.2"
ARG BUILD_NUMBER="dev"
ARG JAVA_VERSION="11"
ARG TOMCAT_VERSION="9.0"

#------------------------------------------------------------------------------

FROM maven:3-openjdk-${JAVA_VERSION} as builder
ARG BUILD_REVISION
ARG BUILD_NUMBER
ARG JAVA_VERSION

WORKDIR /app

# Download and cache all Maven Dependencies
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    echo "java.runtime.version=${JAVA_VERSION}" >> system.properties \
    && mvn dependency:go-offline \
        -DbuildNumber='${BUILD_NUMBER}' \
        -Drevision="${BUILD_REVISION}" \
        -Dpostgres.jdbc.scope='compile' \
        -Dversion.check.repository=''

# Install and compile the Java Source code
COPY ./src/ ./src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package verify \
        -DbuildNumber='${BUILD_NUMBER}' \
        -Drevision="${BUILD_REVISION}" \
        -Dpostgres.jdbc.scope='compile' \
        -Dversion.check.repository=''

#------------------------------------------------------------------------------

FROM onaci/tomcat-base:${TOMCAT_VERSION}-jdk${JAVA_VERSION} as server
ARG BUILD_REVISION
ARG BUILD_NUMBER
ARG JAVA_VERSION
ARG TOMCAT_VERSION
LABEL org.opencontainers.image.base.name="onaci/tomcat-base:${TOMCAT_VERSION}-jdk${JAVA_VERSION}"

# Upgrade the base image
ENV DEBIAN_FRONTEND noninteractive
RUN --mount=target=/var/lib/apt/lists,type=cache,sharing=locked \
    --mount=target=/var/cache/apt,type=cache,sharing=locked \
    apt-get update \
    && apt-get -y upgrade \
    && apt-get clean \
    && apt-get autoremove --purge \
    && rm -rf /var/lib/apt/lists/*

# Install the compiled PID Java application
COPY --from=builder "/app/target/pidsvc-${BUILD_REVISION}.${BUILD_NUMBER}.war" "${CATALINA_HOME}/webapps/pidsvc.war"

# Install the tomcat context definition that sets up the database connection
COPY ./docker-init/tomcat/context.xml "${CATALINA_HOME}/conf/Catalina/localhost/pidsvc.xml"

# Allow the runtime Postgresql database connectionstring to be configured via
# docker configs or docker secrets (or plain old )
COPY ./docker-init/tomcat/configure-pidsvc-db.sh "${CATALINA_HOME}/conf/"
RUN echo ". '${CATALINA_HOME}/conf/configure-pidsvc-db.sh'" >> "${CATALINA_HOME}/bin/setenv.sh"
