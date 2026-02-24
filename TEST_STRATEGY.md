# 테스트 전략 문서

## 1. 검증할 행위 목록

### 선정 기준

행위를 선정할 때 다음 기준을 적용했다.

1. **사용자 관점의 외부 행위인가?** - 내부 구현(메서드 호출, 필드 변경)이 아닌 API 경계에서 관찰 가능한 결과를 검증 대상으로 선택했다.
2. **비즈니스적으로 보호할 가치가 있는가?** - 리팩터링 시에도 반드시 유지되어야 하는 행위를 우선했다.
3. **성공과 실패 양쪽을 모두 다루는가?** - 정상 흐름만이 아니라, 실패 시 시스템 상태가 안전하게 유지되는지도 검증했다.

### 검증 행위 목록

이 프로젝트의 핵심 도메인은 **선물하기(Gift)**이다. API가 노출된 3개 도메인(Gift, Product, Category) 중 선물하기가 재고 변경이라는 부수효과를 수반하는 유일한 비즈니스 로직이므로, 인수 테스트의 주 대상으로 선택했다.

| # | 행위 | 분류 | 선정 이유 |
|---|------|------|----------|
| 1 | 선물하면 재고가 감소한다 | 정상 흐름 | 핵심 비즈니스 규칙. 가장 기본적인 행위 |
| 2 | 여러 번 선물하면 재고가 누적 감소한다 | 정상 흐름 | 트랜잭션별 독립성과 누적 효과 검증 |
| 3 | 재고보다 많은 수량 선물 시 실패하고 재고가 유지된다 | 실패 + 불변성 | 과주문 방지 규칙. 실패 시 부수효과 없음을 보장 |
| 4 | 재고 소진 후 추가 선물 시 실패한다 | 실패 흐름 | 재고 0 상태에서의 안전성 검증 |
| 5 | 존재하지 않는 옵션으로 선물 시 실패한다 | 실패 흐름 | 잘못된 입력에 대한 시스템 안정성 |
| 6 | 재고와 정확히 같은 수량 선물 시 성공한다 (경계값) | 경계값 | `<` vs `<=` 조건 오류를 잡는 경계값 테스트 |
| 7 | 수량 0으로 선물하면 재고가 변하지 않는다 | 경계값 | 최솟값 경계에서의 동작 확인 |
| 8 | 음수 수량으로 선물하면 재고가 증가한다 (버그) | 버그 검출 | 입력 검증 누락으로 인한 버그 식별 |
| 9 | 존재하지 않는 발신자로 선물 시 실패하고 재고가 유지된다 | 실패 + 불변성 | 인증/인가 경계에서의 안전성 |
| 10 | 실패 후 정상 선물 시 재고가 올바르게 감소한다 | 복합 시나리오 | 실패가 후속 요청에 영향을 주지 않음을 보장 |

### 단위 테스트와의 역할 분담

| 관점 | 단위 테스트 (OptionTest) | 인수 테스트 (GiftAcceptanceTest) |
|------|------------------------|-------------------------------|
| 검증 대상 | `Option.decrease()` 메서드의 도메인 규칙 | `POST /api/gifts` API의 비즈니스 행위 |
| 검증 방식 | 객체 상태 직접 확인 | HTTP 응답 코드 + DB 상태 재조회 |
| 보호 범위 | 도메인 로직 변경 | 트랜잭션, 영속성, 컨트롤러 연결 포함 전체 흐름 |
| 리팩터링 내성 | 낮음 (내부 구조 변경 시 깨질 수 있음) | 높음 (외부 행위가 같으면 통과) |

---

## 2. 테스트 데이터 전략

### 데이터 준비 방식

**Repository 직접 주입 방식**을 선택했다.

```java
@Autowired private CategoryRepository categoryRepository;
@Autowired private ProductRepository productRepository;
@Autowired private OptionRepository optionRepository;
@Autowired private MemberRepository memberRepository;
```

**이유:**
- 이 프로젝트는 Option/Member에 대한 생성 API가 존재하지 않아, API만으로는 테스트 데이터를 준비할 수 없다.
- Category/Product는 API가 있지만, 테스트 목적은 선물하기 행위 검증이므로 데이터 준비 단계를 간결하게 유지하기 위해 Repository를 사용했다.
- 데이터 준비는 검증 대상이 아닌 사전 조건(Given)이므로, 준비 방식의 단순함을 우선했다.

### 테스트 픽스처

공통 데이터는 `@BeforeEach`에서, 테스트별 데이터는 헬퍼 메서드로 생성한다.

```java
@BeforeEach
void setUp() {
    sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
    receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
}

private Option createOptionWithStock(int stock) {
    Category category = categoryRepository.save(new Category("테스트 카테고리"));
    Product product = productRepository.save(new Product("테스트 상품", 10000, "http://test.jpg", category));
    return optionRepository.save(new Option("테스트 옵션", stock, product));
}
```

- **공통 픽스처 (sender, receiver):** 모든 테스트에 필요한 회원 데이터. `@BeforeEach`에서 매번 생성.
- **가변 픽스처 (Option):** 재고 수량이 테스트 시나리오마다 다르므로, `createOptionWithStock(int stock)` 헬퍼로 필요 시 생성.

### 데이터 격리 전략

```java
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
```

- 매 테스트 후 Spring Context를 재생성하여 DB 상태를 완전히 초기화한다.
- H2 인메모리 DB를 사용하므로 Context 재생성 시 모든 데이터가 사라진다.
- **트레이드오프:** 테스트 실행 속도가 느려지지만, 테스트 간 완벽한 격리를 보장한다. 현재 테스트 수(10개)에서는 허용 가능한 수준이다.

---

## 3. 검증 전략

### 검증 대상: HTTP 응답 + DB 상태

인수 테스트에서는 두 가지를 함께 검증한다.

1. **HTTP 응답 코드** - 사용자에게 전달되는 결과
2. **DB 상태 (재고 수량)** - 시스템 내부 상태의 정합성

```java
// 성공 케이스: 응답 OK + 재고 감소 확인
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(getStock(option.getId())).isEqualTo(7);

// 실패 케이스: 응답 에러 + 재고 불변 확인
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
assertThat(getStock(option.getId())).isEqualTo(5);
```

### HTTP 응답만으로 충분하지 않은 이유

- `200 OK`를 받았더라도 재고가 실제로 감소했는지는 알 수 없다.
- `500 Error`를 받았더라도 재고가 이미 변경되었을 수 있다 (트랜잭션 롤백 실패 등).
- 따라서 **응답 코드와 DB 상태를 함께 검증**하여 행위의 완전성을 확인한다.

### DB 상태 확인 방법

```java
private int getStock(Long optionId) {
    return optionRepository.findById(optionId)
            .orElseThrow()
            .getQuantity();
}
```

- Repository를 통해 DB에서 직접 재조회한다.
- API 재호출(GET)이 아닌 Repository 직접 조회를 선택한 이유: Option 조회 API가 존재하지 않기 때문이다.

### 실패 시 불변성 검증

실패 케이스에서는 "실패했을 때 부수효과가 없는가"를 반드시 검증한다.

```java
// 재고 부족 → 실패 + 재고 원래대로
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
assertThat(getStock(option.getId())).isEqualTo(5);  // 변하지 않음
```

이는 트랜잭션 롤백이 제대로 동작하는지, 실패한 요청이 시스템 상태를 오염시키지 않는지를 확인하는 중요한 검증이다.

---

## 4. 주요 의사결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 테스트 대상 API | `POST /api/gifts` 중심 | 재고 변경이라는 부수효과가 있는 유일한 핵심 비즈니스 로직 |
| 데이터 준비 | Repository 직접 주입 | Option/Member 생성 API 부재. 준비 단계의 간결함 우선 |
| 테스트 격리 | `@DirtiesContext` | 완벽한 격리 보장. 테스트 수가 적어 속도 트레이드오프 허용 |
| 검증 방식 | HTTP 응답 + Repository 재조회 | Option 조회 API 부재. 응답만으로는 상태 변경을 확인할 수 없음 |
| 버그 테스트 | 현재 동작 문서화 | 음수 수량 입력 시 재고 증가 버그를 테스트로 식별하고 기록 |
