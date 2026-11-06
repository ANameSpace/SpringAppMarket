FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn
COPY mvnw pom.xml settings.xml ./

COPY --parents **/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && \
    ./mvnw dependency:go-offline -B -s ./settings.xml

COPY app-service/src ./app-service/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -s ./settings.xml -pl app-service package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /build/app-service/target/*.jar /app/application.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
