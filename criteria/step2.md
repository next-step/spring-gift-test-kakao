# Step 2 - PostgreSQL 전환 요구사항 충족 여부

## 요구사항 체크리스트

| # | 요구사항 | 충족 여부 |
|---|---------|----------|
| 1 | Docker Compose로 PostgreSQL 실행 환경 구성 | O |
| 2 | Spring 프로파일로 테스트/개발 DB 분리 | O |
| 3 | 테스트 실행 시 DB 자동 시작 및 체크 | O |
| 4 | 각 시나리오마다 DB 초기화 (Test Isolation) | O |

---

## 1. Docker Compose로 PostgreSQL 실행 환경 구성

**충족 여부**: O

### 근거

#### docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:17
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: test
      POSTGRES_PASSWORD: test
    tmpfs:
      - /var/lib/postgresql/data
```

- PostgreSQL 17 이미지를 사용한다.
- `tmpfs`로 데이터를 메모리에 저장하여 디스크 I/O 없이 빠르게 동작한다.
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경 변수로 초기 설정을 자동화한다.

#### build.gradle 의존성

```gradle
runtimeOnly 'com.h2database:h2'
runtimeOnly 'org.postgresql:postgresql'
```

- PostgreSQL JDBC 드라이버를 추가하고, 기존 H2는 개발용으로 유지한다.

---

## 2. Spring 프로파일로 테스트/개발 DB 분리

**충족 여부**: O

### 근거

#### 개발 환경 (`application.properties`)

```properties
spring.application.name=gift
spring.jpa.open-in-view=false
```

- 별도 DB 설정이 없으므로 Spring Boot가 H2를 자동 구성한다.

#### 테스트 환경 (`src/test/resources/application-test.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

- `test` 프로파일 활성화 시 PostgreSQL에 접속한다.
- `ddl-auto=create`로 애플리케이션 시작 시 테이블을 자동 생성한다.

#### 테스트 클래스에서 프로파일 활성화

모든 테스트 클래스(7개)에 `@ActiveProfiles("test")`를 적용했다.

| 클래스 | 위치 |
|--------|------|
| `CucumberSpringConfiguration` | `gift.acceptance` |
| `GiftServiceTest` | `gift.application` |
| `CategoryServiceTest` | `gift.application` |
| `ProductServiceTest` | `gift.application` |
| `OptionServiceTest` | `gift.application` |
| `WishServiceTest` | `gift.application` |
| `FakeGiftDeliveryTest` | `gift.infrastructure` |

---

## 3. 테스트 실행 시 DB 자동 시작 및 체크

**충족 여부**: O

### 근거

#### Gradle 태스크 구성 (`build.gradle`)

```gradle
tasks.register('startDb', Exec) {
    commandLine 'docker', 'compose', 'up', '-d', '--wait'
}

tasks.register('stopDb', Exec) {
    commandLine 'docker', 'compose', 'down'
}

tasks.named('test') {
    dependsOn 'startDb'
    finalizedBy 'stopDb'
    useJUnitPlatform()
}

tasks.register('cucumberTest', Test) {
    dependsOn 'startDb'
    finalizedBy 'stopDb'
    useJUnitPlatform {
        includeEngines 'junit-platform-suite'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    include '**/CucumberTest*'
    testLogging {
        events 'passed', 'failed', 'skipped'
        showStandardStreams = false
        exceptionFormat 'short'
    }
}
```

- `dependsOn 'startDb'`: 테스트 실행 전 `docker compose up -d --wait`로 PostgreSQL을 시작하고 healthy 상태까지 대기한다.
- `finalizedBy 'stopDb'`: 테스트 성공/실패 관계없이 `docker compose down`으로 컨테이너를 정리한다.
- `cucumberTest` 태스크로 Cucumber 인수 테스트만 별도 실행할 수 있다.

### 실행 흐름

```
./gradlew test          → startDb → test → stopDb
./gradlew cucumberTest  → startDb → cucumberTest → stopDb
```

### cucumberTest 태스크 설계 선택

`includeEngines 'junit-platform-suite'`를 명시한 이유: `CucumberTest.java`가 `@Suite` 어노테이션으로 `junit-platform-suite` 엔진에 속하기 때문이다. `includeEngines 'cucumber'`로 설정하면 Cucumber 엔진이 직접 `.feature` 파일을 디스커버리하려고 시도하지만 실패하여 테스트가 0개 실행된다. Suite 엔진이 `CucumberTest` 클래스를 발견한 후 내부적으로 Cucumber 엔진에 위임하는 구조이므로, `junit-platform-suite`를 지정해야 한다. 다만 `include '**/CucumberTest*'`로 이미 클래스 필터링이 되므로, `includeEngines`는 의도를 명시하는 역할이다.

---

## 4. 각 시나리오마다 DB 초기화 (Test Isolation)

**충족 여부**: O

### 근거

#### Cucumber `@Before` 훅 (`CommonSteps.java`)

```java
@Before
public void cleanUp() {
    optionRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    memberRepository.deleteAll();
}
```

- 각 시나리오 실행 전에 모든 테이블의 데이터를 삭제한다.
- 외래 키 제약 조건을 고려하여 자식 테이블부터 삭제한다 (Option -> Product -> Category, Member).

#### ScenarioContext의 `cucumber-glue` 스코프

```java
@Bean
@Scope("cucumber-glue")
public ScenarioContext scenarioContext() {
    return new ScenarioContext();
}
```

- `ScenarioContext`는 시나리오마다 새로운 인스턴스가 생성되어 이전 시나리오의 상태가 영향을 주지 않는다.
