# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S insulinet && adduser -S insulinet -G insulinet
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
RUN chown insulinet:insulinet app.jar

USER insulinet

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -qO- http://localhost:${PORT:-8000}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
