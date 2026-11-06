FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn
COPY mvnw pom.xml settings.xml ./

COPY --parents **/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && \
    ./mvnw dependency:go-offline -B -s ./settings.xml

COPY eureka-server/src ./eureka-server/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -s ./settings.xml -pl eureka-server package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /build/eureka-server/target/*.jar /app/application.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/app/application.jar"]