# One Dockerfile per service; the build context is this folder.
#
#   (default target)  jvm     compiles a JVM fast-jar from source with public Maven repositories; used by docker compose and OpenShift Local
#   --target native           NS Jenkins: copies the prebuilt native executable build/app-runner onto the NS UBI8 image
#                             (--build-arg NATIVE_BASE=nsos-pr1-nex.atldc.nscorp.com:8083/ubi8)

# Base image of the native target. The default is public because OpenShift pulls the base image of every stage, even one it does
# not build, and cannot reach the NS registry.
ARG NATIVE_BASE=registry.access.redhat.com/ubi8/ubi

# ── Stage 1: build (JVM, from source) ─────────────────────────────────────────
FROM gradle:9.1.0-jdk25 AS build

WORKDIR /build

# Public equivalents of the NS Nexus repos (Central + Gradle Plugin Portal for the io.quarkus plugin) and a JVM fast-jar
# instead of the native package type some services set in gradle.properties. Services that use Maven Central ignore the repository properties.
ARG GRADLE_OVERRIDES="-PnsRepository=https://repo.maven.apache.org/maven2 \
    -PnsSharedJarsRepository=https://plugins.gradle.org/m2 \
    -PquarkusPlatformGroupId=io.quarkus.platform \
    -PquarkusPlatformVersion=3.39.5 \
    -Dquarkus.package.type=fast-jar"

# Cache dependency resolution separately from source compilation
COPY settings.gradle build.gradle gradle.properties ./
RUN gradle --no-daemon $GRADLE_OVERRIDES dependencies -q > /dev/null || true

COPY src ./src
RUN gradle --no-daemon $GRADLE_OVERRIDES build -x test -q

# ── Target native: NS Jenkins, prebuilt native executable on UBI8 ─────────────
FROM ${NATIVE_BASE} AS native

# OpenShift builds every stage, so this must not fail when there is no native binary: the build folder is copied only when it exists
# (it does for the Jenkins build, which has just run ./gradlew build) and app-runner is taken out of it.
COPY . /tmp/module/
RUN if [ -f /tmp/module/build/app-runner ]; then cp /tmp/module/build/app-runner ./app-runner && chmod +x ./app-runner; fi; rm -rf /tmp/module

EXPOSE 8080

ENTRYPOINT ["./app-runner"]

# ── Target jvm (default): runtime image ───────────────────────────────────────
FROM eclipse-temurin:25-jre AS jvm

WORKDIR /app

# Copy the Quarkus fast-jar runner and its lib directory
COPY --from=build /build/build/quarkus-app/lib             ./lib
COPY --from=build /build/build/quarkus-app/quarkus-run.jar ./quarkus-run.jar
COPY --from=build /build/build/quarkus-app/app             ./app
COPY --from=build /build/build/quarkus-app/quarkus          ./quarkus

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
