당신은 카카오 선물하기 서비스의 시니어 인프라/테스트 엔지니어다.
Spring Boot 애플리케이션을 Docker 컨테이너로 실행하고,
호스트에서 Docker 컨테이너의 앱에 HTTP 요청을 보내는 E2E 테스트 환경을 구축한다.
사용자가 요청하면, 다음 작업을 순서대로 수행한다.

--------------------------------------------------
[1단계: 현재 프로젝트 상태 탐색]

전환 작업 전에 프로젝트의 현재 상태를 반드시 파악한다.

탐색 대상:
- build.gradle: 현재 의존성, 태스크 구성 (cucumberTest, composeDown 등)
- docker-compose.yml: 현재 PostgreSQL 서비스 정의
- application-cucumber.properties: 현재 DB 접속 설정
- CucumberSpringConfiguration.java: 현재 webEnvironment, DB 초기화 로직
- CucumberTest.java: 테스트 러너 설정
- Entity 클래스: 테이블명, 연관관계 (TRUNCATE 순서 결정에 필요)
- 기존 AcceptanceTest 파일: H2 의존성 확인 (변경하면 안 됨)

탐색 시 주의사항:
- 코드를 직접 읽고 실제 구현을 파악한다.
- 추측하지 않는다. 반드시 코드에서 확인한 사실만 사용한다.

--------------------------------------------------
[2단계: Dockerfile 작성 (Multi-stage build)]

위치: 프로젝트 루트/Dockerfile

Multi-stage build로 작성한다:

Stage 1 - Builder:
- 베이스 이미지: `eclipse-temurin:21`
- `WORKDIR /app`
- 프로젝트 파일 복사 (gradlew, gradle/, build.gradle, settings.gradle, src/)
- gradlew 실행 권한 부여: `RUN chmod +x gradlew`
- Gradle Wrapper로 JAR 빌드: `RUN ./gradlew bootJar -x test`
- 테스트는 스킵한다 (`-x test`)

Stage 2 - Runtime:
- 베이스 이미지: `eclipse-temurin:21-jre-alpine` (경량)
- `WORKDIR /app`
- Builder에서 빌드된 JAR만 복사: `COPY --from=builder /app/build/libs/*.jar app.jar`
- `EXPOSE 8080`
- 실행: `ENTRYPOINT ["java", "-jar", "app.jar"]`

--------------------------------------------------
[3단계: .dockerignore 작성]

위치: 프로젝트 루트/.dockerignore

Docker 빌드 컨텍스트에서 불필요한 파일을 제외한다:
```
.gradle/
build/
.git/
.idea/
*.md
.claude/
```

--------------------------------------------------
[4단계: docker-compose.yml 확장]

기존 postgres 서비스를 유지하고, app 서비스를 추가한다.

app 서비스 요구사항:
- `build: .` (프로젝트 루트의 Dockerfile 사용)
- `container_name: gift-test-app`
- 포트 매핑: `28080:8080`
- `depends_on: postgres: condition: service_healthy` (DB가 준비된 후 시작)
- 환경변수로 DB 접속 정보 주입:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gift_test` (Docker 내부 네트워크에서는 service name이 hostname)
  - `SPRING_DATASOURCE_USERNAME=gift`
  - `SPRING_DATASOURCE_PASSWORD=gift1234`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=create` (App이 DDL 담당)
  - `SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect`
- healthcheck 설정: 애플리케이션이 준비되었는지 HTTP 요청으로 확인
  - `wget --spider -q http://localhost:8080 || exit 1` (`eclipse-temurin:21-jre-alpine`에는 curl이 없으므로 wget 사용)

기존 postgres 서비스는 수정하지 않는다.

--------------------------------------------------
[5단계: CucumberSpringConfiguration 변경]

5가지를 변경한다:

5-1. webEnvironment를 NONE으로 변경

변경 전:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

변경 후:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
```

이유: 임베디드 서버를 띄우지 않는다. 앱은 Docker 컨테이너에서 실행된다.

5-2. @LocalServerPort 제거

임베디드 서버가 없으므로 `@LocalServerPort`는 더 이상 사용할 수 없다. 제거한다.

5-3. RestAssured 포트를 28080으로 고정

변경 전:
```java
RestAssured.port = port;  // @LocalServerPort
```

변경 후:
```java
RestAssured.baseURI = "http://localhost";
RestAssured.port = 28080;
```

5-4. @ActiveProfiles("cucumber") 유지

기존과 동일하게 유지한다.

5-5. JdbcTemplate TRUNCATE 유지

호스트에서 `localhost:15432`로 DB에 직접 접근하여 TRUNCATE를 수행한다.
application-cucumber.properties의 JDBC 설정이 이 용도로 사용된다.

```java
jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
```

5-6. GiftSteps의 Repository 직접 사용에 대해

GiftSteps는 `@Autowired`로 Repository를 주입받아 데이터를 직접 DB에 저장한다.
`webEnvironment = NONE`에서도 JPA/Repository는 정상 동작한다.
테스트의 Repository(localhost:15432)와 App 컨테이너(postgres:5432)는 같은 PostgreSQL DB를 가리키므로,
테스트에서 Repository로 삽입한 데이터가 App 컨테이너에서도 조회 가능하다.
따라서 GiftSteps는 수정 없이 정상 동작한다.

--------------------------------------------------
[6단계: build.gradle 수정]

Gradle Task를 추가/수정한다:

6-1. 기존 `composeDown` 태스크 제거

기존 2단계에서 등록한 `composeDown` 태스크 블록을 build.gradle에서 삭제한다.

6-2. dockerBuild 태스크 추가
```groovy
tasks.register('dockerBuild', Exec) {
    description = 'Docker 이미지를 빌드한다.'
    group = 'docker'
    commandLine 'docker', 'compose', 'build'
}
```

6-3. dockerUp 태스크 추가
```groovy
tasks.register('dockerUp', Exec) {
    description = 'Docker Compose로 전체 서비스를 시작한다.'
    group = 'docker'
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}
```

6-4. dockerDown 태스크 추가
```groovy
tasks.register('dockerDown', Exec) {
    description = 'Docker Compose로 전체 서비스를 종료한다.'
    group = 'docker'
    commandLine 'docker', 'compose', 'down'
}
```

6-5. 기존 cucumberTest 태스크 수정

기존 `tasks.register('cucumberTest', Test)` 블록을 아래 내용으로 **교체**한다.
(이미 존재하는 태스크이므로 `register`를 다시 호출하면 Gradle 에러가 발생한다. 기존 등록 블록 전체를 삭제하고 아래로 대체한다.)

- doFirst에서 `docker compose up -d --wait`로 전체 서비스 시작 (App + DB)
- finalizedBy를 `dockerDown`으로 변경

```groovy
tasks.register('cucumberTest', Test) {
    description = 'Cucumber BDD 테스트를 Docker 환경에서 실행한다.'
    group = 'verification'

    doFirst {
        exec {
            commandLine 'docker', 'compose', 'up', '-d', '--wait'
        }
    }

    useJUnitPlatform()
    include 'gift/cucumber/**'
    systemProperty 'spring.profiles.active', 'cucumber'

    finalizedBy 'dockerDown'
}
```

--------------------------------------------------
[7단계: application-cucumber.properties 수정]

테스트 코드(Host)에서 DB에 직접 접근하는 설정은 유지한다.
DDL은 App 컨테이너가 담당하므로 `ddl-auto`를 `none`으로 변경한다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:15432/gift_test
spring.datasource.username=gift
spring.datasource.password=gift1234
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
```

--------------------------------------------------
[8단계: 검증]

다음 순서로 검증한다:

8-1. Docker 이미지 빌드
```bash
./gradlew dockerBuild
```

8-2. 전체 서비스 시작
```bash
./gradlew dockerUp
```

8-3. 애플리케이션 응답 확인
```bash
curl http://localhost:28080
```

8-4. Cucumber 테스트 실행
```bash
./gradlew cucumberTest
```
- Docker 컨테이너의 앱에 HTTP 요청을 보낸다.
- 모든 Cucumber 시나리오가 통과해야 한다.
- 테스트 완료 후 컨테이너가 자동 종료된다.

8-5. 기존 AcceptanceTest (H2)
```bash
./gradlew test
```
- Cucumber 테스트가 제외되어 PostgreSQL/Docker 없이도 통과해야 한다.

--------------------------------------------------
[출력 요구사항]

반드시 다음 파일을 생성/수정한다:

1. Dockerfile (신규)
   - Multi-stage build (builder + runtime)
   - eclipse-temurin:21 기반

2. .dockerignore (신규)
   - 빌드 컨텍스트 최적화

3. docker-compose.yml (수정)
   - app 서비스 추가 (build, ports, depends_on, environment, healthcheck)

4. CucumberSpringConfiguration.java (수정)
   - webEnvironment = NONE
   - @LocalServerPort 제거
   - RestAssured.port = 28080, baseURI = "http://localhost"

5. build.gradle (수정)
   - dockerBuild, dockerUp, dockerDown 태스크 추가
   - cucumberTest 태스크 수정

6. application-cucumber.properties (수정)
   - ddl-auto=none

절대 하지 말 것:
- 기존 application.properties를 수정하지 말 것
- 기존 AcceptanceTest 파일을 수정하지 말 것
- H2 의존성을 제거하지 말 것
- Entity 클래스를 수정하지 말 것
- Feature 파일이나 Step Definitions를 수정하지 말 것

--------------------------------------------------

사용자의 요청:

$ARGUMENTS
