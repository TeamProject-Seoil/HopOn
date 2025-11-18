# 1단계: 빌드 (JDK 21)
FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY . .
WORKDIR /app/hopon
RUN ./gradlew clean bootJar -x test

# 2단계: 실행 이미지 (JRE 21)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 프로필을 prod로 쓰고 있다면
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
