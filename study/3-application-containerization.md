# 요구사항 3: Application 컨테이너화

## Application 컨테이너화가 뭔가?

우리 Spring Boot 앱을 Docker 이미지로 만들어서, Docker 컨테이너 안에서 실행하는 것이다. 개발자 PC에 Java가 없어도 Docker만 있으면 앱을 실행할 수 있다.

```
기존: java -jar app.jar (Host에서 직접 실행)
변경: Docker 컨테이너 안에서 java -jar app.jar
```

## Multi-stage build는 무엇이고 왜 사용하는가?

Dockerfile을 두 단계로 나눠서, 빌드 도구는 최종 이미지에 포함시키지 않는 기법이다.

```dockerfile
# 1단계: 빌드 (JDK 필요 — 무거움)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src/main ./src/main
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행 (JRE만 필요 — 가벼움)
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- build 스테이지: JDK + Gradle로 jar 빌드. 이 스테이지는 최종 이미지에 포함 안 됨
- run 스테이지: JRE-alpine(경량)으로 jar만 실행. `COPY --from=build`로 빌드 결과만 가져옴
- 결과: JDK, Gradle, 소스코드 없이 **jar + JRE만 있는 가벼운 이미지**

### 레이어 캐싱

```dockerfile
COPY gradlew settings.gradle build.gradle ./    # 변경 적음
COPY gradle ./gradle                             # 변경 적음
RUN ./gradlew dependencies --no-daemon           # 캐싱됨 (의존성 안 바뀌면)
COPY src/main ./src/main                          # 소스 변경 시 여기부터 재실행
RUN ./gradlew bootJar --no-daemon -x test        # 재빌드
```

의존성 다운로드 레이어를 분리해서, 소스만 변경되면 bootJar만 재실행된다. 빌드 시간이 크게 줄어든다.

## Docker 네트워크에서 service name이 어떻게 hostname이 되는가?

Docker Compose는 같은 네트워크 안의 서비스끼리 **서비스 이름으로 통신**할 수 있게 해준다.

```yaml
services:
  db: # ← 이 이름이 hostname
    image: postgres:17
  app:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/gift_test
      #                                       ^^
      #                                  서비스 이름 = hostname
```

- 컨테이너 → 컨테이너: `db:5432` (서비스 이름)
- Host → 컨테이너: `localhost:5432` (포트 매핑)

## 테스트 아키텍처

```
테스트 (Host) → HTTP → localhost:28080 (Docker App) → JDBC → db:5432 (Docker DB)
테스트 (Host) → JDBC → localhost:5432 (Docker DB)   ← DB cleanup
```

- **테스트 프로세스**: Host의 JVM에서 실행. RestAssured로 HTTP 요청을 보냄
- **App 컨테이너**: Docker에서 실행. port 28080 → 8080 매핑
- **DB 컨테이너**: Docker에서 실행. port 5432 → 5432 매핑
- **DB cleanup**: 테스트 프로세스가 localhost:5432로 직접 TRUNCATE 실행

## webEnvironment = NONE을 사용하는 이유는?

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

앱이 이미 Docker 컨테이너(28080)에서 돌고 있으므로, 테스트 프로세스에서 웹서버를 또 띄울 필요가 없다. Spring 컨텍스트(JPA, DataSource)만 로드해서 DB cleanup에 사용한다.

| 설정            | 웹서버  | 용도                         |
|---------------|------|----------------------------|
| `RANDOM_PORT` | 띄움   | 앱이 Host에서 실행될 때 (요구사항 1)   |
| `NONE`        | 안 띄움 | 앱이 Docker에서 실행될 때 (요구사항 3) |

## 프로파일 분리

```
application.properties              ← 공통 (앱 이름, kakao API)
application-dev.properties          ← bootRun: localhost:5432/gift_dev, ddl-auto=update
application-docker-test.properties  ← Docker App 컨테이너: db:5432/gift_test, ddl-auto=update
application-cucumber.properties     ← 테스트 프로세스: localhost:5432/gift_test, ddl-auto=none
```

- `docker-test`: App 컨테이너 전용. `db:5432` (Docker 내부 통신), `ddl-auto=update` (테이블 생성)
- `cucumber`: 테스트 프로세스 전용. `localhost:5432` (Host에서 접근), `ddl-auto=none` (TRUNCATE만)
- `dev`: 로컬 개발 전용. `localhost:5432/gift_dev`

### cucumber 프로파일은 왜 ddl-auto=none인가?

테이블 생성은 App 컨테이너(`ddl-auto=update`)가 담당한다. 테스트 프로세스가 `create-drop`을 쓰면 App 컨테이너가 만든 테이블을 날려버린다. 테스트 프로세스는 TRUNCATE만 하면
된다.

## docker compose build vs docker compose up

```bash
docker compose build    # Dockerfile로 이미지 빌드 (소스 → jar → 이미지)
docker compose up -d    # 이미지로 컨테이너 실행
```

build는 이미지를 만드는 것, up은 이미지를 실행하는 것. 소스가 변경되면 build를 다시 해야 변경사항이 반영된다.

## ./gradlew bootRun vs IntelliJ Run 버튼

|                        | `./gradlew bootRun`     | IntelliJ Run 버튼          |
|------------------------|-------------------------|--------------------------|
| 실행 방식                  | Gradle 태스크              | IntelliJ가 `main()` 직접 실행 |
| `dependsOn dockerDbUp` | 실행됨 (DB 자동 시작)          | 안 됨                      |
| `dev` 프로파일             | `systemProperty`로 자동 적용 | 별도 설정 필요                 |

IntelliJ Run 버튼은 Gradle을 거치지 않으므로 `build.gradle`에 설정한 `dependsOn`, `systemProperty`가 적용되지 않는다.

## H2 인메모리 DB 설정은 어디 있는가?

별도 설정이 없다. Spring Boot auto-configuration이 classpath에 H2 의존성이 있고 다른 datasource 설정이 없으면 자동으로 H2 인메모리 DB를 구성한다.

```groovy
runtimeOnly 'com.h2database:h2'  // 이 의존성만 있으면 자동 설정
```
