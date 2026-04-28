FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

RUN chmod +x ./mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /workspace/target/zigzag-shop-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
