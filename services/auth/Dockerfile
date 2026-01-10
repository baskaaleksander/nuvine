FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -T 1C dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app && \
    apk add --no-cache dumb-init

WORKDIR /app

COPY --from=builder /app/target/auth-*.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8040

USER app

ENTRYPOINT ["dumb-init", "--", "java"]
CMD ["-jar", "/app/app.jar"]