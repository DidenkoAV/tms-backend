FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN mkdir -p /app/logs \
    && addgroup --system spring \
    && adduser --system --ingroup spring spring \
    && chown -R spring:spring /app

COPY --from=builder /build/target/testcase-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8083

USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
