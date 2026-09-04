# Build and run on the JDK the pom targets (<java.version> in pom.xml).
# openjdk:* is deprecated on Docker Hub; eclipse-temurin is its replacement.
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven wrapper and pom.xml first (for better layer caching)
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make the Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose the port that the application runs on
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "target/archiving-0.0.1-SNAPSHOT.jar"]
