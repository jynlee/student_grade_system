# =============================================================================
# 백엔드 컨테이너 빌드 — 멀티스테이지 빌드
# Stage 1: Maven으로 WAR 빌드
# Stage 2: Tomcat 공식 이미지에 WAR 복사
# =============================================================================

# ----- Stage 1: Build -----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 의존성 캐싱을 위해 pom.xml 먼저 복사
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 소스 복사 후 빌드
COPY src ./src
RUN mvn clean package -DskipTests

# ----- Stage 2: Runtime -----
FROM tomcat:9-jdk17
# Tomcat 기본 webapps 정리 (ROOT 등 제거 — 우리 WAR만 둠)
RUN rm -rf /usr/local/tomcat/webapps/*
# 빌드된 WAR를 ROOT.war로 복사 → 컨텍스트 패스 / (루트)
COPY --from=builder /build/target/students-*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
