# ---------- Stage 1: Build ----------
# 프로젝트 Gradle 버전에 맞춤 (8.10.2 + JDK17)
FROM gradle:8.10.2-jdk17-alpine AS builder

WORKDIR /app

# Gradle 캐시 최대 활용: 우선 빌드 스크립트와 래퍼만 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (실패해도 캐시 유지용으로 통과)
RUN gradle dependencies --no-daemon || true

# 애플리케이션 소스 복사 후 빌드 (테스트 제외: 프리티어 메모리 절약)
COPY . .
RUN gradle clean bootJar --no-daemon -x test


# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

  # ✅ 필수 패키지 설치 (wget, tzdata, ffmpeg)
RUN apk add --no-cache wget tzdata ffmpeg && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 비루트 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 빌드 산출물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]

EXPOSE 8080