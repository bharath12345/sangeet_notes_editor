# Sangeet Notes Editor — server Docker image for Cloud Run / any container host.
#
# Build:   docker build -t sangeet-server .
# Run:     docker run -p 28080:28080 sangeet-server
# Submit:  gcloud builds submit --tag gcr.io/PROJECT_ID/server

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the assembled fat JAR built by `sbt sangeetServer/assembly`.
# The glob handles the Scala-version suffix in the artifact name.
COPY sangeet-server/target/scala-3.*/sangeet-server-assembly-*.jar /app/server.jar

# Cloud Run injects PORT via env. EmberServer reads it via the application.
ENV PORT=28080
EXPOSE 28080

# Run with a fixed heap; Cloud Run gives 512 Mi by default but we leave room
# for native + metaspace.
ENTRYPOINT ["java", "-Xms256m", "-Xmx400m", "-jar", "/app/server.jar"]
