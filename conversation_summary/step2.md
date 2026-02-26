## 대화 요약

### 요약 주제
H2 → PostgreSQL 전환 및 Docker Compose 테스트 환경 자동화

### 핵심 내용
- H2 in-memory DB를 PostgreSQL로 전환하고, Docker Compose로 테스트 환경을 자동화함
- Spring 프로파일(`test`)로 개발/테스트 DB를 분리하고, 모든 테스트 클래스에 `@ActiveProfiles("test")` 적용
- Gradle 태스크(`startDb`, `stopDb`)로 테스트 실행 시 PostgreSQL 자동 시작/종료 구현
- Cucumber 전용 `cucumberTest` 태스크를 추가하여 인수 테스트만 별도 실행 가능하게 함

---

### 1단계: PostgreSQL 전환 기본 설정

#### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `build.gradle` | `runtimeOnly 'org.postgresql:postgresql'` 추가 |
| `docker-compose.yml` | PostgreSQL 17 + tmpfs 설정 (신규) |
| `src/test/resources/application-test.properties` | PostgreSQL 접속 정보 (신규) |

#### 선택: Docker Compose 방식

PostgreSQL 실행 방법으로 **Docker Compose 수동 방식**을 선택함. Testcontainers는 사용하지 않음.

- `docker-compose.yml`에 `tmpfs`를 적용하여 디스크 I/O 없이 H2에 가까운 속도를 확보함
- `ddl-auto`는 처음 `create-drop`으로 설정했으나, 테스트 후 테이블을 확인할 수 있도록 `create`로 변경함

#### 선택: Spring 프로파일로 DB 분리

`@ActiveProfiles("test")`를 7개 테스트 클래스에 개별 적용하는 방식을 선택함. 커스텀 어노테이션(`@IntegrationTest`) 방식은 사용하지 않음.

---

### 2단계: Gradle 태스크로 DB 자동 시작/종료

#### 선택: startDb + stopDb 태스크

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
}
```

- `dependsOn`: 테스트 전 PostgreSQL 자동 시작 + healthy 체크
- `finalizedBy`: 테스트 성공/실패 관계없이 컨테이너 자동 정리

---

### 3단계: cucumberTest 태스크 추가

#### 선택: junit-platform-suite 엔진 + CucumberTest 클래스 필터링

```gradle
tasks.register('cucumberTest', Test) {
    dependsOn 'startDb'
    finalizedBy 'stopDb'
    useJUnitPlatform {
        includeEngines 'junit-platform-suite'
    }
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    include '**/CucumberTest*'
}
```

#### 핵심 설계 결정: includeEngines 선택

`includeEngines 'cucumber'`가 아닌 `'junit-platform-suite'`를 사용한 이유:

- `CucumberTest.java`는 `@Suite` 어노테이션으로 `junit-platform-suite` 엔진에 속함
- `includeEngines 'cucumber'`로 설정하면 Cucumber 엔진이 `.feature` 파일을 직접 디스커버리하려 하지만 실패하여 테스트 0개 실행됨
- Suite 엔진이 `CucumberTest` 클래스를 발견한 후 내부적으로 Cucumber 엔진에 위임하는 구조
- `include '**/CucumberTest*'`로 이미 클래스 필터링이 되므로 `includeEngines`는 사실상 의도를 명시하는 역할

#### 콘솔 출력 설정

`src/test/resources/junit-platform.properties`에 `cucumber.plugin=pretty`를 추가하여 시나리오별 결과를 콘솔에 출력함. `testLogging` 블록은 `cucumber.plugin=pretty`와 중복되므로 제거함.

`cucumber.properties`가 아닌 `junit-platform.properties`에 설정한 이유: Cucumber JUnit Platform Engine이 `junit-platform.properties`에서 설정을 읽기 때문.

---

### 결정사항

| 항목 | 선택 | 대안 | 선택 이유 |
|------|------|------|----------|
| PostgreSQL 실행 방식 | Docker Compose | Testcontainers | 수동 제어 가능, 별도 의존성 불필요 |
| DB 분리 방식 | `@ActiveProfiles("test")` 개별 적용 | 커스텀 `@IntegrationTest` 어노테이션 | 단순함, 추가 추상화 불필요 |
| ddl-auto | `create` | `create-drop` | 테스트 후 DB 테이블 확인 가능 |
| DB 자동화 | Gradle startDb/stopDb 태스크 | Testcontainers 자동 관리 | Gradle 레벨에서 제어, Docker Compose 방식과 일관성 |
| cucumberTest 엔진 | `junit-platform-suite` | `cucumber` | `@Suite` 기반 CucumberTest 클래스가 suite 엔진에 속하기 때문 |
| 콘솔 출력 설정 위치 | `junit-platform.properties` | `cucumber.properties` | JUnit Platform Engine이 해당 파일에서 설정을 읽음 |
| 테스트 격리 | 기존 `@Before` + `deleteAll()` 유지 | TRUNCATE CASCADE, `@Sql` | 현 단계에서 변경하지 않기로 결정 |
