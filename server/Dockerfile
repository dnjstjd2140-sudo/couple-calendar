FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
EXPOSE 8081
# 서버 RAM 2GB 를 telpo 와 나눠 쓰므로 힙을 작게 제한
ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-jar", "app.jar"]
