# Cucumber 테스트 자동화 - Docker & PostgreSQL

## 아키텍처 변화

### 기존 방식 - Local H2 테스트

- Test Code, 앱, DB 모두 하나의 JVM 내에서 실행

### 변경 후 - Docker + PostgreSQL

- Test Runner : Gradle이 로컬 JVM에서 실행
- 앱 : Docker 컨테이너로 격리
- DB : Docker 컨테이너로 격리

- 테스트 코드가 HTTP 통신으로 Docker 내부 앱 호출 
    - 앱 (localhost:28080) 
    - DB (localhost:25432)

## Gradle 태스크 자동화

- 테스트 실행 시 Docker 컨테이너 자동으로 관리(Up/Down)하고, cucumber 프로필을 강제.

```Groovy

tasks.register('dockerDown', Exec) {
    group = 'docker'
    description = 'Docker Compose 종료 및 컨테이너 삭제'
    commandLine 'docker-compose', 'down'
}

// Cucumber 테스트 전용
tasks.register('cucumberTest', Test) {
    group = 'verification'
    description = 'Docker DB 띄우고 Cucumber 인수 테스트 진행'

    // 커스텀 Test 태스크가 테스트 클래스를 찾을 수 있도록 설정
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath

    // Cucumber 엔진만 실행 (RestAssured 등 다른 테스트 제외)
    useJUnitPlatform {
        includeEngines 'cucumber'
    }

    // 시스템 프로퍼티 설정
    systemProperty 'spring.profiles.active', 'cucumber'

    // 테스트 실행 전 docker-compose up (--build로 앱 이미지 재빌드 포함)
    doFirst {
        exec {
            commandLine 'docker-compose', 'up', '-d', '--build', '--wait'
        }
    }

    // 테스트 성공/실패 상관없이 종료 후 Docker 내리기
    finalizedBy dockerDown

    // test와 동시 실행 시 순서 보장
    shouldRunAfter test
}
```

## 테스트 환경 설정

- 테스트 실행 시 application-cucumber.properties 설정 읽어오도록 프로필 명시.
    - @ActiveProfiles("cucumber")

## 데이터베이스 초기화 SQL 변경

- H2와 달리 PostgreSQL에서는 FK 제약 조건이 상대적으로 엄격해서  TRUNCATE ... CASCADE 문법을 사용해야 한다.

## 테스트 분리 - excludeEngines 적용

- test 태스크에서 cucumber 테스트 제외

```Groovy
tasks.named('test') {
  useJUnitPlatform {
    excludeEngines 'cucumber'
  }
}
```

## Dockerfile (Multi-stage Build) 분석

```Dockerfile
# Stage 1 : 빌드 도구 포함 무거운 이미지
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
# Window 줄바꿈 문자 제거 후 빌드
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew clean build -x test --no-daemon

# Stage 2 : 실행에 필요한 최소한의 Runtime 정보 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 앞 단계에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Docker Compose 분석

```YAML
services:
  # DB service
  postgres: # 서비스 이름
    image: postgres:15
    container_name: gift-postgres
    ports:
      - "25432:5432"
    environment:
      POSTGRES_DB: gift
      POSTGRES_USER: gift
      POSTGRES_PASSWORD: gift
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gift"]
      interval: 5s
      retries: 5

  # 앱 service
  app:
    build: . # 현재 디렉토리 Dockerfile로 빌드
    container_name: gift-app
    ports:
      - "28080:8080" # (Host:Container) 호스트 28080 -> 컨테이너 8080
    environment:
      # 서비스 이름 -> 호스트 이름
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift
    depends_on:
      postgres:
        condition: service_healthy # DB가 healthy 상태 될 때까지 시작 대기
```


## 테스트 시 내장 Tomcat 비활성화

- webEnvironment = SpringBootTest.WebEnvironment.NONE 적용

