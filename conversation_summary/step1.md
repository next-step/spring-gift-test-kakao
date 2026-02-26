## 대화 요약

### 요약 주제
Cucumber 테스트 전환 과정 전체 요약

### 핵심 내용
- 기존 RestAssured 기반 테스트 4개 파일(18개 시나리오)을 Cucumber 기반으로 전환하면서 시나리오 차이를 분석함
- 전환 과정에서 **카테고리 1개, 상품 2개 시나리오가 누락**된 것을 발견하고 보완함
- GiftAcceptanceTest와 GiftControllerTest 사이에 **8개 메서드가 중복**되어 있었으며, Cucumber 전환 시 4개로 통합됨
- 질문을 통해 어노테이션 정합성, feature 키워드, `@Before` 훅 위치 등 **4가지 개선**을 추가로 진행함

---

### 1단계: 초기 비교 및 누락 시나리오 보완

#### 누락되었던 시나리오 (보완 완료)

| 도메인 | 추가된 시나리오 | 파일 |
|--------|----------------|------|
| 카테고리 | 여러 카테고리를 생성하면 모두 조회된다 | `category.feature` |
| 상품 | 상품이 없으면 빈 목록을 반환한다 | `product.feature` |
| 상품 | 여러 상품을 생성하면 모두 조회된다 | `product.feature` |

#### 중복이었던 테스트 메서드

**GiftAcceptanceTest** (5개 전부 중복):
- `선물_전송_성공_시_200을_반환한다`
- `선물_전송_성공_시_재고가_차감된다`
- `존재하지_않는_옵션으로_전송하면_500을_반환한다`
- `재고_부족_시_500을_반환한다`
- `재고_부족_시_재고가_변경되지_않는다`

**GiftControllerTest** (4개 중 3개 중복):
- `선물_전송_API_성공`
- `존재하지_않는_옵션이면_500_에러`
- `재고가_부족하면_500_에러`

**중복 아닌 메서드** (GiftControllerTest에만 존재):
- `Member_Id_헤더가_없으면_400_에러`

#### 보완 후 최종 비교

| 도메인 | 삭제된 테스트 | Cucumber (보완 후) | 차이 |
|--------|:---:|:---:|------|
| 카테고리 | 4개 | 4개 | 0 |
| 상품 | 5개 | 5개 | 0 |
| 선물 | 9개 (2파일) | 4개 | -5 (중복 제거) |
| **합계** | **18개** | **13개** | **-5 (순수 중복 제거분)** |

---

### 2단계: 질문을 통한 개선사항

#### 개선 1: `@And` → `@Given`/`@When`/`@Then` 어노테이션 정리

**문제**: Step 메서드가 의미와 무관하게 `@And`로 선언되어 있었음

**변경**:

| 파일 | 메서드 | 변경 전 | 변경 후 |
|------|--------|:---:|:---:|
| CategorySteps | 응답에 카테고리가 포함되어 있다 | `@And` | `@Then` |
| CategorySteps | 카테고리 목록에 이름이 포함되어 있다 | `@And` | `@Then` |
| CategorySteps | 카테고리 목록은 비어있다 | `@And` | `@Then` |
| CategorySteps | 카테고리 목록에 N개가 있다 | `@And` | `@Then` |
| ProductSteps | 응답에 상품이 포함되어 있다 | `@And` | `@Then` |
| ProductSteps | 상품 목록에 이름이 포함되어 있다 | `@And` | `@Then` |
| ProductSteps | 상품이 등록되어 있다 | `@And` | `@Given` |
| ProductSteps | 상품 목록은 비어있다 | `@And` | `@Then` |
| ProductSteps | 상품 목록에 N개가 있다 | `@And` | `@Then` |
| GiftSteps | 옵션이 재고 N개로 등록되어 있다 | `@And` | `@Given` |
| GiftSteps | 보내는 회원과 받는 회원이 등록되어 있다 | `@And` | `@Given` |
| GiftSteps | 옵션 재고가 N개로 차감되어 있다 | `@And` | `@Then` |
| GiftSteps | 옵션 재고가 N개로 변경되지 않았다 | `@And` | `@Then` |

#### 개선 2: `.feature` 파일 키워드를 한글 → 영문으로 변경

**문제**: `# language: ko`로 한글 키워드(`기능`, `시나리오`, `만일`, `그러면` 등)를 사용하고 있었음

**변경**: `Feature`, `Scenario`, `Background`, `Given`, `When`, `Then`, `And`로 통일. 설명과 스텝 내용은 한글 유지.

#### 개선 3: `.feature` 키워드와 Java 어노테이션 일치시키기

**문제**: 같은 스텝 텍스트(`"카테고리를 생성하면"`)가 feature 파일에서 `Given`과 `When` 양쪽에 쓰여 어노테이션 불일치 발생

**변경**: 전제 조건과 행위를 다른 표현으로 분리

| 역할 | feature 키워드 | 스텝 텍스트 | 어노테이션 |
|------|:---:|---|:---:|
| 전제 조건 | `Given` | ~가 **등록되어 있다** | `@Given` |
| 행위 | `When` | ~를 **생성하면** | `@When` |

#### 개선 4: `@Before` 훅을 CommonSteps로 통합

**문제**: `@Before`가 CategorySteps와 GiftSteps에 흩어져 있어 데이터 정리 로직 파악이 어려움. Cucumber의 `@Before`는 모든 시나리오에서 실행되므로 분리할 이유가 없음.

**변경 전**:
```
CategorySteps.@Before → option, product, category 삭제
GiftSteps.@Before     → member 삭제
```

**변경 후**:
```
CommonSteps.@Before   → option, product, category, member 전부 삭제
```

#### 개선 5: DB 초기화를 ORM `deleteAll()`에서 SQL `TRUNCATE CASCADE`로 전환

**문제**: ORM `deleteAll()`은 FK 제약조건 때문에 삭제 순서를 수동 관리해야 하고, 엔티티 추가 시 코드 수정이 필요함.

**변경 전**:
```java
optionRepository.deleteAll();
productRepository.deleteAll();
categoryRepository.deleteAll();
memberRepository.deleteAll();
```

**변경 후**:
```java
jdbcTemplate.execute("TRUNCATE wish, option, product, category, member CASCADE");
```

**선택 이유**: `TRUNCATE CASCADE`는 FK 순서에 관계없이 한 번에 초기화되고, 테이블 추가 시 한 줄만 수정하면 됨.

---

### 결정사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 누락 시나리오 보완 | category.feature에 1개, product.feature에 2개 추가 | 삭제된 테스트와 동일한 행위 커버리지 확보 |
| 선물 중복 테스트 통합 | 9개 → 4개로 통합 유지 | 2개 파일에 걸친 순수 중복이므로 통합이 적절 |
| 어노테이션 패턴 | `@Given`/`@When`/`@Then`으로 의미 구분 | 가독성 향상, feature 키워드와 일치 |
| feature 키워드 | 영문 키워드 사용 | 표준 Gherkin 키워드로 통일 |
| 표현 분리 | "등록되어 있다"(Given) vs "생성하면"(When) | 하나의 패턴이 하나의 어노테이션에만 대응되도록 |
| `@Before` 위치 | CommonSteps에 통합 | 한 곳에서 데이터 정리 로직 파악 가능 |
| DB 초기화 방식 | `TRUNCATE CASCADE` (JdbcTemplate) | FK 순서 무관, 엔티티 추가 시 유지보수 용이 |

