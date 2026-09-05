# ---- Build stage ------------------------------------------------------------
# Needs the full JDK (javac) and the Maven wrapper. None of this reaches the
# final image. Build on the JDK the pom targets (<java.version> in pom.xml);
# openjdk:* is deprecated on Docker Hub, eclipse-temurin is its replacement.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /build

# Copy the Maven wrapper and pom.xml first (for better layer caching)
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make the Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies. Keyed on pom.xml alone, so editing source does not
# re-resolve the whole tree.
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
# Only a JRE and the jar: no compiler, no Maven, no ~/.m2 cache, no sources.
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy just the repackaged fat jar. The wildcard avoids hardcoding the project
# version, so bumping <version> in pom.xml cannot silently break the image.
# (spring-boot:repackage leaves the pre-repackage jar as *.jar.original, which
# this does not match.)
COPY --from=build /build/target/*.jar app.jar

# Port for the `docker` profile, which compose.yaml activates via
# SPRING_PROFILES_ACTIVE (see application-docker.properties: server.port=8080).
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
