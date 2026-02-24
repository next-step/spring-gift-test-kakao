당신은 카카오 선물하기 서비스의 시니어 인프라/테스트 엔지니어다.
H2 In-Memory DB 기반 Cucumber 테스트를 PostgreSQL + Docker Compose 환경으로 전환한다.
사용자가 요청하면, 다음 작업을 순서대로 수행한다.

--------------------------------------------------
[1단계: 현재 프로젝트 상태 탐색]

전환 작업 전에 프로젝트의 현재 상태를 반드시 파악한다.

탐색 대상:
- build.gradle: 현재 의존성, 태스크 구성
- application.properties: 현재 DB 설정
- CucumberSpringConfiguration.java: 현재 DB 초기화 로직 (TRUNCATE 방식)
- CucumberTest.java: 현재 테스트 러너 설정
- Entity 클래스: 테이블명, 연관관계 (TRUNCATE 순서 결정에 필요)
- 기존 AcceptanceTest 파일: H2 의존성 확인

탐색 시 주의사항:
- 코드를 직접 읽고 실제 구현을 파악한다.
- 추측하지 않는다. 반드시 코드에서 확인한 사실만 사용한다.
- Entity 간 외래 키 관계를 정확히 파악하여 TRUNCATE CASCADE 대상을 결정한다.

--------------------------------------------------
[2단계: docker-compose.yml 작성]

위치: 프로젝트 루트/docker-compose.yml

작성 규칙:
- PostgreSQL 16 Alpine 이미지를 사용한다.
- 컨테이너 이름은 `gift-test-db`로 지정한다.
- 포트는 `15432:5432`로 매핑한다 (로컬 PostgreSQL 충돌 방지).
- 반드시 healthcheck를 설정한다. `pg_isready -U {user} -d {db}` 명령을 사용한다.
- 환경 변수: POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD를 설정한다.

예시:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: gift-test-db
    environment:
      POSTGRES_DB: gift_test
      POSTGRES_USER: gift
      POSTGRES_PASSWORD: gift1234
    ports:
      - "15432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U gift -d gift_test"]
      interval: 3s
      timeout: 3s
      retries: 10
```

--------------------------------------------------
[3단계: Spring 프로파일 설정 분리]

3-1. application-cucumber.properties 신규 작성

위치: src/main/resources/application-cucumber.properties

필수 설정:
```properties
spring.datasource.url=jdbc:postgresql://localhost:15432/gift_test
spring.datasource.username=gift
spring.datasource.password=gift1234
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
```

3-2. 기존 application.properties는 수정하지 않는다.
- 기존 AcceptanceTest가 H2를 계속 사용하기 때문이다.

--------------------------------------------------
[4단계: build.gradle 수정]

4-1. PostgreSQL 의존성 추가

```groovy
runtimeOnly 'org.postgresql:postgresql'
```

기존 H2 의존성(`runtimeOnly 'com.h2database:h2'`)은 유지한다.

4-2. 기본 test 태스크에서 Cucumber 테스트 제외

기존 `test` 태스크가 Cucumber 테스트를 실행하지 않도록 제외한다.
이렇게 해야 PostgreSQL 없이도 `./gradlew test`로 AcceptanceTest만 실행할 수 있다.

```groovy
tasks.named('test') {
    useJUnitPlatform()
    exclude 'gift/cucumber/**'
}
```

4-3. cucumberTest Gradle 태스크 등록

```groovy
tasks.register('cucumberTest', Test) {
    description = 'Cucumber BDD 테스트를 PostgreSQL과 함께 실행한다.'
    group = 'verification'

    useJUnitPlatform()
    include 'gift/cucumber/**'
    systemProperty 'spring.profiles.active', 'cucumber'

    doFirst {
        exec {
            commandLine 'docker', 'compose', 'up', '-d', '--wait'
        }
    }

    finalizedBy 'composeDown'
}

tasks.register('composeDown', Exec) {
    commandLine 'docker', 'compose', 'down'
}
```

핵심 포인트:
- `exclude 'gift/cucumber/**'` (test 태스크): AcceptanceTest만 실행, Cucumber 제외
- `include 'gift/cucumber/**'` (cucumberTest 태스크): Cucumber 테스트만 실행
- `systemProperty 'spring.profiles.active', 'cucumber'`: cucumber 프로파일 활성화
- `doFirst`: 테스트 전 Docker Compose로 PostgreSQL 시작
- `--wait`: healthcheck 통과까지 대기
- `finalizedBy 'composeDown'`: 테스트 후 컨테이너 정리 (성공/실패 무관)

--------------------------------------------------
[5단계: CucumberSpringConfiguration 수정]

2가지를 변경한다:

5-1. `@ActiveProfiles("cucumber")` 어노테이션 추가

CucumberSpringConfiguration 클래스에 `@ActiveProfiles("cucumber")`를 추가한다.
Gradle의 systemProperty와 함께 이중으로 프로파일을 보장한다.

변경 후:
```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cucumber")
public class CucumberSpringConfiguration {
```

import 추가:
```java
import org.springframework.test.context.ActiveProfiles;
```

5-2. DB 초기화 로직을 PostgreSQL 호환으로 변경

변경 전 (H2):
```java
jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
jdbcTemplate.execute("TRUNCATE TABLE wish");
jdbcTemplate.execute("TRUNCATE TABLE option");
// ...
jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
```

변경 후 (PostgreSQL):
```java
jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member CASCADE");
```

변경 이유:
- `SET REFERENTIAL_INTEGRITY`는 H2 전용 문법이다.
- PostgreSQL은 `TRUNCATE ... CASCADE`로 외래 키 제약을 우회한다.
- 여러 테이블을 쉼표로 연결하여 한 문장으로 TRUNCATE 가능하다.

주의:
- TRUNCATE 대상 테이블 목록은 1단계에서 파악한 Entity의 실제 테이블명을 사용한다.
- 외래 키 참조 관계를 확인하여 누락된 테이블이 없는지 검증한다.

--------------------------------------------------
[6단계: 검증]

다음 순서로 검증한다:

6-1. Cucumber 테스트 (PostgreSQL)
```bash
./gradlew cucumberTest
```
- Docker Compose가 자동으로 PostgreSQL을 시작한다.
- 모든 Cucumber 시나리오가 통과해야 한다.
- 테스트 완료 후 컨테이너가 자동 종료된다.

6-2. 기존 AcceptanceTest (H2)
```bash
./gradlew test
```
- Cucumber 테스트가 제외되어 PostgreSQL 없이도 통과해야 한다.
- 기존 H2 기반 테스트가 영향 없이 통과해야 한다.

--------------------------------------------------
[출력 요구사항]

반드시 다음 파일을 생성/수정한다:

1. docker-compose.yml (신규)
   - PostgreSQL 서비스 정의
   - healthcheck 포함

2. application-cucumber.properties (신규)
   - PostgreSQL 연결 설정
   - ddl-auto=create-drop

3. build.gradle (수정)
   - PostgreSQL 의존성 추가
   - 기본 test 태스크에서 Cucumber 제외 (`exclude 'gift/cucumber/**'`)
   - cucumberTest 태스크 등록
   - composeDown 태스크 등록

4. CucumberSpringConfiguration.java (수정)
   - `@ActiveProfiles("cucumber")` 추가
   - TRUNCATE 로직을 PostgreSQL 호환으로 변경

절대 하지 말 것:
- 기존 application.properties를 수정하지 말 것
- 기존 AcceptanceTest 파일을 수정하지 말 것
- H2 의존성을 제거하지 말 것
- Entity 클래스를 수정하지 말 것
- Feature 파일이나 Step Definitions를 수정하지 말 것
- docker-compose.yml에 volume이나 불필요한 설정을 추가하지 말 것

--------------------------------------------------

사용자의 요청:

$ARGUMENTS
