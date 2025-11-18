# 1단계: Maven 빌드 (JDK 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1) 서브폴더 hopon 안의 pom.xml만 먼저 복사 → 캐시용
COPY hopon/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline

# 2) 소스 코드 복사
COPY hopon/src ./src

# 3) 실제 빌드 (테스트는 일단 스킵)
RUN mvn -B clean package -DskipTests

# 2단계: 실행 이미지 (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 위에서 빌드된 jar 복사 (/app/target/*.jar)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
