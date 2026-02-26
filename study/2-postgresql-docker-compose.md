# 요구사항 2: PostgreSQL + Docker Compose 통합

## 왜 H2 대신 PostgreSQL을 사용하는가?

Production Parity(프로덕션 동등성) 때문이다. H2는 인메모리 DB로 빠르지만, 실제 운영 환경의 PostgreSQL과 동작이 다를 수 있다. 같은 DB를 쓰면 "로컬에서는 되는데 운영에서 안 되는"
문제를 사전에 잡을 수 있다.

## healthcheck는 무엇인가?

컨테이너가 "실행 중"인 것과 "요청을 받을 준비가 된 것"은 다르다.

```yaml
db:
  healthcheck:
    test: [ "CMD", "pg_isready", "-U", "gift" ]
    interval: 2s
    timeout: 5s
    retries: 10
```

- `pg_isready`: PostgreSQL이 연결을 수락할 준비가 됐는지 확인하는 명령
- 2초마다 확인하고, 10번까지 재시도
- healthy 상태가 되어야 다음 서비스(`app`)가 시작된다

healthcheck가 없으면 DB가 아직 준비 안 됐는데 앱이 연결을 시도해서 실패할 수 있다.

## Spring Profile은 어떻게 동작하는가?

`application-{프로파일}.properties` 파일이 활성 프로파일에 따라 로드된다.

```
application.properties          ← 항상 로드 (공통 설정)
application-dev.properties      ← bootRun 시 (gift_dev DB)
application-cucumber.properties ← cucumberTest 시 (gift_test DB, 테스트 프로세스)
application-docker-test.properties ← Docker 앱 컨테이너 (gift_test DB)
```

활성화 방법:

- `build.gradle`: `systemProperty 'spring.profiles.active', 'dev'`
- `docker-compose.yml`: `SPRING_PROFILES_ACTIVE: docker-test`
- 기본값(프로파일 없음): `application.properties`만 로드 → H2 auto-configuration

## Gradle Task에서 Docker를 어떻게 실행하는가?

`Exec` 타입 태스크로 셸 명령을 실행한다.

```groovy
task dockerUp(type: Exec) {
    commandLine '/usr/local/bin/docker', 'compose', 'up', '-d', '--wait'
}
```

`--wait`: 모든 서비스의 healthcheck가 통과할 때까지 기다린다. 이게 없으면 컨테이너가 시작만 되고 준비가 안 된 상태에서 테스트가 돌 수 있다.

## 테스트 실패 시에도 DB가 정리되는가?

`finalizedBy`가 이를 보장한다.

```groovy
task cucumberTest(type: Test) {
    dependsOn dockerBuild, dockerUp
    finalizedBy dockerDown
}
```

- `dependsOn`: cucumberTest 실행 전에 반드시 실행
- `finalizedBy`: cucumberTest가 성공하든 실패하든 반드시 실행

테스트가 실패해도 `dockerDown`이 실행되어 컨테이너가 정리된다.

## dependsOn, finalizedBy, mustRunAfter 차이

```groovy
cucumberTest.dependsOn dockerUp       // cucumberTest → dockerUp 반드시 먼저 실행
cucumberTest.finalizedBy dockerDown   // cucumberTest 끝나면 (성공/실패 무관) dockerDown 실행
dockerUp.mustRunAfter dockerBuild     // 둘 다 실행될 때만 순서 보장 (의존성 아님)
```

- `dependsOn`: "이거 없으면 실행 못 함" (강제 의존)
- `finalizedBy`: "끝나면 무조건 실행" (정리 용도)
- `mustRunAfter`: "같이 실행되면 이 순서로" (순서만 보장)

## H2 테스트와 PostgreSQL 테스트를 어떻게 분리하는가?

```groovy
tasks.named('test') {
    exclude '**/CucumberTest*'    // Cucumber 제외
}

task cucumberTest(type: Test) {
    include '**/CucumberTest*'    // Cucumber만 실행
}
```

| 명령어                      | DB         | 테스트                                 |
|--------------------------|------------|-------------------------------------|
| `./gradlew test`         | H2         | CategoryTest, ProductTest, GiftTest |
| `./gradlew cucumberTest` | PostgreSQL | CucumberTest (Cucumber 시나리오)        |

## DB Cleanup은 어떻게 동작하는가?

`DatabaseCleanup`이 H2와 PostgreSQL을 분기 처리한다.

```java
// PostgreSQL (cucumber 프로파일)
tableNames =jdbcTemplate.

queryForList(
    "SELECT table_name FROM information_schema.tables "+
            "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",String .class);

for(
String tableName :tableNames){
        jdbcTemplate.

execute("TRUNCATE TABLE \""+tableName +"\" CASCADE");
}

// H2 (프로파일 없음)
        jdbcTemplate.

execute("SET REFERENTIAL_INTEGRITY FALSE");
for(
String tableName :tableNames){
        jdbcTemplate.

execute("TRUNCATE TABLE "+tableName);
}
        jdbcTemplate.

execute("SET REFERENTIAL_INTEGRITY TRUE");
```

- PostgreSQL: `TRUNCATE CASCADE`로 외래키 포함 전체 삭제
- H2: `REFERENTIAL_INTEGRITY`를 끄고 TRUNCATE 후 다시 켬
- `@Before` hook에서 매 시나리오 전에 실행 → 시나리오 간 데이터 격리
