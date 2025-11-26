ARG BUILDER_JDK_VERSION=amazoncorretto:21-al2023-headless
FROM ${BUILDER_JDK_VERSION} AS builder

WORKDIR /application

ARG MODULE
ARG JAR_FILE=${MODULE}/build/libs/*.jar

COPY ${JAR_FILE} application.jar

RUN java -Djarmode=layertools -jar application.jar extract

ARG BUILDER_JDK_VERSION
FROM ${BUILDER_JDK_VERSION}
WORKDIR /application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

