# Study Document

## 1. Cucumber 학습 정리

- Cucumber는 사람이 읽을 수 있는 예시(Examples)를 Gherkin으로 작성하고, 이를 자동화 코드(Step Definitions)와 연결해 **요구사항 ↔ 구현**을 동기화하는 도구다.
- 사용자 행위 중심으로 테스트를 작성할 수 있게 도와준다.
- 다만 테스트를 읽는 주체가 개발자만인 경우, 표현 방식의 이점이 상대적으로 줄 수 있다.
- 자연어 시나리오를 Cucumber가 이해할 수 있게 작성하는 문법이 **Gherkin**이다.

참고:
- [레딧 - Do you use cucumber to write integration tests?](http://reddit.com/r/rails/comments/132217t/do_you_use_cucumber_to_write_integration_tests/?tl=ko)

---

## 2. Gradle `doFirst`의 역할

Gradle 빌드는 크게 구성 단계(configuration phase)와 실행 단계(execution phase)로 나뉜다.

- `doFirst { ... }`는 태스크가 **실제로 실행되는 직전**에 동작한다.
- 따라서 `-Pdb=postgres` 같은 실행 옵션 기반 분기에서, 테스트 시작 직전에 `systemProperty`를 확정 세팅할 때 유용하다.
- 의도:
  - "테스트가 진짜 시작되기 직전에" `spring.profiles.active`를 확실히 반영

---

## 3. `@ActiveProfiles` vs `systemProperty 'spring.profiles.active'`

둘 다 프로필 활성화에 관여하지만, 동일한 메커니즘은 아니다.

### 3.1 `@ActiveProfiles("cucumber")`

- Spring TestContext가 테스트 ApplicationContext 생성 시 적용하는 테스트 메타데이터 방식
- 테스트 코드에 가까워 의도가 명확하고, IDE/CI/Gradle 실행 간 일관성 확보에 유리

### 3.2 `systemProperty 'spring.profiles.active'`

- JVM 시스템 프로퍼티로 전달되어 Spring Boot 시작 시 반영되는 외부 입력 방식
- 빌드/CI에서 중앙집중적으로 제어하기 쉬움

### 3.3 결론

- `@ActiveProfiles`와 `systemProperty`는 목적과 적용 경로가 다르다.
- 둘을 함께 쓸 경우 조합 방식에 따라 예상과 다르게 보일 수 있으므로, 프로필 문자열을 명시적으로 관리하는 것이 안전하다.

---

## 4. Cucumber Glue와 `@CucumberContextConfiguration` 충돌 해결

### 4.1 Glue란

Cucumber에서 **glue**는 `.feature` 파일의 Gherkin 문장과 Java 코드를 연결하는 스캔 경로다.
Cucumber는 glue로 지정된 패키지를 스캔하여 다음을 찾는다:

- **Step Definitions** — `@Given`, `@When`, `@Then`이 붙은 메서드
- **Hooks** — `@Before`, `@After` 등 생명주기 훅
- **`@CucumberContextConfiguration`** — Spring ApplicationContext를 부트스트랩하는 설정 클래스

### 4.2 문제: `@CucumberContextConfiguration`은 glue 내에 정확히 1개만 허용

Cucumber-Spring은 glue 스캔 범위 안에서 `@CucumberContextConfiguration`이 붙은 클래스를 **하나만** 허용한다.
2개 이상 발견되면 `CucumberBackendException`이 발생한다.

테스트 실행 모드를 H2 / PostgreSQL / Docker로 나누면서 각 모드마다 다른 `@SpringBootTest` 설정이 필요했다:

| 모드 | webEnvironment | ActiveProfiles |
|------|---------------|----------------|
| H2 | `RANDOM_PORT` | `cucumber` |
| PostgreSQL | `RANDOM_PORT` | `cucumber, postgres` |
| Docker | `NONE` | `cucumber, docker` |

3개의 `@CucumberContextConfiguration` 클래스가 필요한데, 이들이 같은 glue 범위에 있으면 충돌한다.

### 4.3 Glue 스캔 범위를 지정하는 방법

**1) `@ConfigurationParameter` (Runner 클래스에서)**

```java
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "gift.cucumber.steps,gift.cucumber.config.h2"
)
```

JUnit Platform Suite Runner에서 Cucumber에 glue 경로를 전달한다.
가장 명시적이며, Runner 클래스 단위로 glue 범위를 다르게 설정할 수 있다.

**2) `cucumber.properties` 파일**

```properties
cucumber.glue=gift.cucumber
```

클래스패스 루트에 위치하며 전역 기본값으로 동작한다. Runner별 분기가 불가능하다.

**3) Gradle `systemProperty`**

```groovy
systemProperty 'cucumber.glue', 'gift.cucumber'
```

JVM 시스템 프로퍼티로 전달. `@ConfigurationParameter`보다 **우선순위가 높아서** Runner의 설정을 덮어쓴다.
따라서 Runner별로 glue를 다르게 지정하려면 Gradle에서 `cucumber.glue`를 설정하면 안 된다.

### 4.4 해결: 패키지 분리 + Runner별 glue 지정

Config 클래스를 **서로 겹치지 않는 패키지**에 배치하고, 각 Runner가 자신의 config 패키지만 glue에 포함하도록 했다.

```
gift.cucumber.config.h2/       → H2ContextConfiguration
gift.cucumber.config.postgres/ → PostgresContextConfiguration
gift.cucumber.config.docker/   → DockerContextConfiguration
gift.cucumber.steps/           → 모든 Step Definitions + Hooks (공유)
```

Runner별로 glue에 `steps` + 자신의 `config` 패키지만 포함:

```java
// RunCucumberH2Test
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
    value = "gift.cucumber.steps,gift.cucumber.config.h2")

// RunCucumberPostgresTest
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
    value = "gift.cucumber.steps,gift.cucumber.config.postgres")

// RunCucumberContainerTest
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
    value = "gift.cucumber.steps,gift.cucumber.config.docker")
```

이렇게 하면 각 Runner의 glue 스캔 범위에는 `@CucumberContextConfiguration`이 정확히 1개만 존재하므로 충돌이 발생하지 않는다.

### 4.5 주의: Gradle `includeEngines`와의 상호작용

`cucumberTest` 태스크에서 `includeEngines 'cucumber'`를 사용하면 Cucumber 엔진이 직접 실행되어 Runner 클래스의 `@ConfigurationParameter`가 무시된다.
Runner의 glue 설정을 존중하려면 `useJUnitPlatform()`만 사용하고 `includeEngines`는 지정하지 않아야 한다.
이 경우 JUnit Platform Suite 엔진이 Runner 클래스를 통해 Cucumber를 간접 실행하므로, `@ConfigurationParameter`가 정상 적용된다.

---
