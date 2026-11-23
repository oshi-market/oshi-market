# 1단계: 빌드 (테스트는 CI·로컬에서 돌리고 여기서는 건너뜀 — contextLoads가 DB를 요구해서)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 (JDK 없이 JRE만 → 이미지가 작아짐)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# 작은 EC2(2GB)에 DB·Nginx와 같이 올리므로 힙을 컨테이너 메모리의 절반으로 제한
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
