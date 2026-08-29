# Use Java 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy maven files first (for caching)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Copy source code
COPY src src

# Build the project
RUN ./mvnw clean package -DskipTests

# Run the jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/instant-chat-1.0.0.jar"]