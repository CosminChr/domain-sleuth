# Backend Dockerfile
FROM openjdk:21-slim

# Set working directory
WORKDIR /app

# Copy the pre-built JAR file (assuming you build it locally first)
COPY build/libs/*.jar app.jar

# Configure JVM options
ENV JAVA_OPTS="-Xmx512m"

# Expose port 8080 and debug port 5005
EXPOSE 8080
EXPOSE 5005

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
