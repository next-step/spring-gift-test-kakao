# Cucumber BDD 적용 코드 리뷰

## 총평

Cucumber BDD 전환이 전체적으로 잘 이루어졌습니다. 한글 Gherkin 시나리오의 가독성이 높고, Spring Boot 통합 설정·데이터 격리·Step Definitions 구조 모두 요구사항에 부합합니다. 16개 테스트 전수 통과(100%)를 확인했습니다. 다만 몇 가지 설계·안정성 측면에서 개선할 여지가 있습니다.

---

## 요구사항 충족 체크리스트

| 요구사항 | 충족 여부 | 비고 |
|---|---|---|
| Gherkin 형식 한글 Given-When-Then | ✅ | `# language: ko`로 조건/만일/그러면 사용 |
| Cucumber + Spring Boot 통합 | ✅ | `@CucumberContextConfiguration` + `@SpringBootTest(RANDOM_PORT)` |
| Step Definitions 구현 | ✅ | `io.cucumber.java.ko` 패키지의 `@조건/@만일/@그러면` 사용 |
| 시나리오 간 데이터 격리 | ✅ | `@Before` hook + `@ScenarioScope` |
| `./gradlew test` 통과 | ✅ | 16 tests, 0 failures, 100% |
| README.md 실행 방법 추가 | ✅ | Cucumber BDD 시나리오 섹션 포함 |
| 학습 내용 문서화 (선택) | ✅ | `docs/requirements/STEP2.md`에 단계별 정리 |

---

## 발견된 문제점

### 🟡 경고: ScenarioContext의 시퀀스 카운터가 DB IDENTITY와 동기화를 가정한다

**파일:** `ScenarioContext.java:25-28`

```java
private long categorySeq = 1;
private long productSeq = 1;
private long optionSeq = 1;
private long memberSeq = 1;
```

**문제:** `CucumberHooks`에서 `ALTER TABLE ... RESTART WITH 1`로 IDENTITY를 리셋하기 때문에 현재는 동작하지만, `ScenarioContext`의 `nextCategoryId()` 등이 반환하는 값이 실제 DB가 생성할 AUTO_INCREMENT 값과 **항상 일치한다는 암묵적 가정**에 의존합니다. Step Definition에서 JdbcTemplate INSERT 시 이 카운터 값을 ID로 직접 넣고 있으므로 지금은 문제없지만, 만약 INSERT 순서가 바뀌거나 일부 Step에서 API를 통해 데이터를 생성하면(API는 IDENTITY로 ID가 자동 할당됨) 카운터와 실제 ID가 어긋날 수 있습니다.

**개선 방향:** JdbcTemplate INSERT 후 `CALL IDENTITY()`나 `SimpleJdbcInsert`를 사용해 실제 생성된 ID를 받아오면 더 안전합니다.

```java
// 예시: H2에서 마지막 INSERT된 ID 조회
jdbcTemplate.update("INSERT INTO category (name) VALUES (?)", name);
Long id = jdbcTemplate.queryForObject("CALL IDENTITY()", Long.class);
scenarioContext.putCategoryId(name, id);
```

---

### 🟡 경고: CucumberHooks에서 cleanup SQL이 하드코딩되어 있다

**파일:** `CucumberHooks.java:23-33`

기존 AcceptanceTest는 `@Sql("/sql/cleanup.sql")`로 외부 SQL 파일을 참조하는데, CucumberHooks에서는 DELETE문과 IDENTITY 리셋을 Java 코드에 직접 하드코딩했습니다.

**부작용:** 테이블이 추가되거나 cleanup 로직이 변경될 때 `cleanup.sql`과 `CucumberHooks`를 **이중으로 관리**해야 합니다. 두 곳이 불일치하면 한쪽 테스트만 실패하는 디버깅하기 어려운 상황이 발생합니다.

**개선된 코드:**

```java
@Before
public void setUp() {
    RestAssured.port = port;

    // cleanup.sql 파일을 읽어서 실행 — 단일 소스 유지
    var resource = new ClassPathResource("/sql/cleanup.sql");
    ScriptUtils.executeSqlScript(
        Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection(),
        resource
    );
}
```

또는 `ResourceDatabasePopulator`를 사용할 수도 있습니다.

---

### 🟡 경고: gift.feature에서 "재고 차감" 결과를 상태코드로만 검증한다

**파일:** `gift.feature:10-14`

```gherkin
시나리오: 선물을 보내면 재고가 차감된다
  만일 "보내는사람"이 "받는사람"에게 "Tall" 옵션으로 7개를 선물하면
  그러면 응답 상태코드는 200이다
  만일 "보내는사람"이 "받는사람"에게 "Tall" 옵션으로 5개를 선물하면
  그러면 응답 상태코드는 500이다
```

**문제:** 시나리오 이름이 "재고가 차감된다"이지만, 실제로 재고가 차감되었는지(10→3) 직접 검증하지 않습니다. 후속 요청의 성공/실패로 간접 추론하는 방식입니다. CLAUDE.md의 테스트 규칙("결과 상태를 검증")에는 부합하지만, BDD 시나리오의 **의도 전달력**이 떨어집니다.

**개선 방향:** 재고 조회 API가 없는 현재 상황에서는 한계가 있지만, DB 직접 조회 Step을 추가하면 시나리오가 더 명확해집니다.

```gherkin
그러면 "Tall" 옵션의 재고는 3개이다
```

---

### 🟢 개선 권장: Step Definitions의 재사용성을 높일 수 있다

**파일:** `CategoryStepDefinitions.java:29-38`, `ProductStepDefinitions.java:48-60`

`카테고리를_생성하면`과 `존재하지_않는_카테고리로_상품을_생성하면`은 API 호출 패턴이 유사합니다. 현재 구조로도 충분히 동작하지만, 향후 시나리오가 늘어나면 API 호출을 파라미터화한 공통 헬퍼 메서드를 고려할 수 있습니다.

또한 `"카테고리 목록에 {string}**가** 포함되어 있다"`와 같은 Step 패턴에서 조사("가/이")가 고정되어 있어, `"케이크"**가**`는 되지만 자연스러운 한국어라면 `"음료"**가**`보다 `"음료"**가**`처럼 조사가 달라질 수 있습니다. 현재는 `{string}`로 감싸져 있어 기능적 문제는 없으나, 가독성 측면에서 인지해두면 좋습니다.

---

### 🟢 개선 권장: GiftStepDefinitions의 회원 등록 Step이 2명으로 고정되어 있다

**파일:** `GiftStepDefinitions.java:30`

```java
@조건("{string}과 {string} 회원이 등록되어 있다")
```

현재 Step은 정확히 2명의 회원만 등록할 수 있습니다. 향후 3명 이상이 필요한 시나리오가 생기면 새 Step을 만들어야 합니다.

**개선 방향:**

```gherkin
조건 "보내는사람" 회원이 등록되어 있다
그리고 "받는사람" 회원이 등록되어 있다
```

이렇게 1명씩 등록하는 Step으로 분리하면 재사용성이 높아집니다.

---

### 🟢 개선 권장: `gift.feature`의 `배경`(Background) 활용은 우수하다

`gift.feature`에서 `배경:` 블록으로 공통 데이터 준비를 분리한 것은 Cucumber의 모범 사례를 잘 따르고 있습니다. 코드 중복이 줄어들고 각 시나리오가 고유 동작에만 집중합니다.

---

### 🟢 개선 권장: `@만일` 키워드가 `그리고`로 연결될 때의 의미 혼동

**파일:** `gift.feature:24-32`

```gherkin
시나리오: 선물을 보낸 후 동일 옵션으로 다시 보내면 누적 차감된다
  만일 "보내는사람"이 "받는사람"에게 "Tall" 옵션으로 3개를 선물하면
  그러면 응답 상태코드는 200이다
  만일 "보내는사람"이 ...4개를 선물하면
  그러면 응답 상태코드는 200이다
  만일 ...4개를 선물하면
  그러면 응답 상태코드는 500이다
  만일 ...3개를 선물하면
  그러면 응답 상태코드는 200이다
```

`만일`(When)과 `그러면`(Then)이 번갈아 나오는 구조입니다. Gherkin 문법적으로는 유효하지만, BDD의 일반적 패턴(Given→When→Then)에서 벗어나 있습니다. 이 경우 의도적으로 "연쇄 동작 검증"을 표현한 것으로 보이며, 기능적으로 문제는 없습니다. 다만, 비개발자에게는 "왜 When-Then이 반복되지?"라는 혼동이 있을 수 있으므로, 필요하다면 시나리오를 분리하거나 주석을 추가하는 것도 방법입니다.

---

## 구조 요약

```
src/test/
├── java/gift/
│   ├── CucumberTest.java                    ← 실행 진입점 (Suite)
│   ├── cucumber/
│   │   ├── CucumberSpringConfiguration.java ← Spring 통합 설정
│   │   ├── CucumberHooks.java               ← @Before: cleanup + 포트 설정
│   │   ├── ScenarioContext.java             ← @ScenarioScope: 시나리오 간 상태 공유
│   │   ├── CommonStepDefinitions.java       ← 공통 Step (상태코드 검증)
│   │   ├── CategoryStepDefinitions.java     ← 카테고리 Step
│   │   ├── ProductStepDefinitions.java      ← 상품 Step
│   │   └── GiftStepDefinitions.java         ← 선물하기 Step
│   ├── AcceptanceTest.java                  ← 기존 베이스 (공존)
│   ├── CategoryAcceptanceTest.java          ← 기존 테스트 (공존)
│   ├── ProductAcceptanceTest.java           ← 기존 테스트 (공존)
│   └── GiftAcceptanceTest.java              ← 기존 테스트 (공존)
└── resources/
    ├── features/
    │   ├── category.feature                 ← 2 시나리오
    │   ├── product.feature                  ← 2 시나리오
    │   └── gift.feature                     ← 4 시나리오 (배경 포함)
    └── cucumber.properties                  ← publish 알림 비활성화
```

---

## 추가 조언

1. **`@ScenarioScope`의 이해**: 현재 `ScenarioContext`에 `@ScenarioScope`를 올바르게 적용했습니다. 이 어노테이션 덕분에 시나리오마다 새로운 인스턴스가 생성되므로 Step Definitions 클래스에 상태를 직접 보관하지 않아도 됩니다. 이 패턴을 유지하세요.

2. **Feature 파일은 "살아있는 문서"**: Feature 파일은 코드인 동시에 비개발 이해관계자를 위한 문서입니다. Step 표현을 수정할 때는 항상 "PM이 읽어도 이해되는가?"를 기준으로 판단하세요.

3. **Cucumber 태그 활용**: 시나리오가 늘어나면 `@smoke`, `@regression` 같은 태그를 붙여 선택적 실행이 가능합니다. 예: `@조건 @critical` → `./gradlew test -Dcucumber.filter.tags="@critical"`

4. **기존 AcceptanceTest와의 공존**: 현재 기존 RestAssured 테스트와 Cucumber 테스트가 동시에 실행됩니다(16 tests = 기존 8 + Cucumber 8). 장기적으로는 한 방식으로 통일하는 것이 유지보수에 유리합니다.

---

**최종 평가: 요구사항을 충실히 충족했으며, 코드 품질도 양호합니다.** `ScenarioContext` 시퀀스 동기화와 cleanup SQL 이중 관리 문제만 개선하면 더 견고한 테스트 인프라가 됩니다.
