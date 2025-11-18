# 1단계: 빌드 (Maven + JDK 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# pom.xml 먼저 복사 → dependency 캐싱
COPY pom.xml .
# dependency만 먼저 다운로드
RUN mvn -B dependency:go-offline

# 나머지 소스 복사
COPY src ./src

# 실제 빌드
RUN mvn -B clean package -DskipTests

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# target/*.jar을 app.jar로 복사
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
