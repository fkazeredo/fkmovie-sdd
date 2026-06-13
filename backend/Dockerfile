# Multi-stage build for the fkmovies backend (modular monolith, Spring Boot 4).
# Build stage uses the project's Maven wrapper so the pinned Maven/JDK is honoured.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -q -B -DskipTests clean package

# Runtime stage: slim JRE + the repackaged executable jar. curl is included only for the
# container HEALTHCHECK against the readiness probe.
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /app/target/fkmovies-backend-*.jar app.jar
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
