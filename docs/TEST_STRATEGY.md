# 인수 테스트 전략 (spring-gift-test-kakao)

## 1. 목표와 범위
- 목표: 사용자 관점의 핵심 시나리오가 안정적으로 동작하는지 검증한다.
- 범위: 현재 외부에 공개된 API(`categories`, `products`, `gifts`)와 선물하기의 재고 변경 규칙을 우선 검증한다.
- 비범위(현 시점): 성능/부하, 보안(인증/인가), 외부 카카오 연동 실구간(현재 FakeGiftDelivery 사용).

## 2. 사전 준비

### 의존성

`build.gradle`에 RestAssured 의존성을 추가한다:

```groovy
testImplementation 'io.rest-assured:rest-assured:5.4.0'
```

### 현재 공개 API 현황

| 엔드포인트                  | 존재 여부         |
|------------------------|---------------|
| `POST /api/categories` | O             |
| `GET /api/categories`  | O             |
| `POST /api/products`   | O             |
| `GET /api/products`    | O             |
| `POST /api/gifts`      | O             |
| Option 생성/조회 API       | X — 공개 API 없음 |
| Member 생성/조회 API       | X — 공개 API 없음 |

> Option과 Member는 Repository를 직접 사용하여 테스트 데이터를 생성한다.

## 3. 검증할 행위 목록

### 선정 기준
- 비즈니스 리스크가 큰가? (잘못되면 금전/재고 오류 발생)
- 사용자 핵심 여정에 포함되는가? (주요 기능 사용 흐름)
- 장애 전파 범위가 큰가? (한 번 실패 시 연쇄 영향)
- 구현 복잡도가 높거나 예외가 많은가?

### P0 (가장 먼저)
1. 선물하기 성공 시 재고가 요청 수량만큼 감소한다.
   - 이유: 핵심 도메인 규칙이며 오류 시 직접적인 비즈니스 손실.

2. 재고보다 많은 수량 선물 시 실패하고 재고는 변경되지 않는다.
   - 이유: 과판매 방지. 실패 시 롤백 보장 필요.

3. 존재하지 않는 옵션으로 선물 시 실패한다.
   - 이유: 참조 무결성/예외 경로 기본 안정성.

### P1 (다음 단계)
4. 카테고리 생성 후 목록 조회 시 포함된다.
5. 상품 생성(유효한 categoryId) 후 목록 조회 시 포함된다.
6. 존재하지 않는 categoryId로 상품 생성 시 실패한다.
   - 이유: 선물 도메인의 사전 데이터(카탈로그) 무결성 보장.

### P2 (확장)
7. 경계값 입력 검증.
   - 수량 0 → `Option.decrease(0)`은 현재 허용됨. 정책 확정 후 테스트 추가.
   - 음수 수량 → 도메인 정책 미정. 입력 검증(Controller/DTO) 레벨에서 차단할지 결정 필요.
8. 오류 응답 형식(상태코드/메시지) 표준화 검증.
   - 현재 `IllegalStateException`에 메시지가 없으므로, 에러 응답 포맷 확정 후 검증.
9. 동시성 시나리오(동일 옵션 동시 선물) 정책 검증.
   - 낙관적 락(`@Version`) 또는 비관적 락(`@Lock`) 도입 후 검증.
   - `ExecutorService`로 동시 요청을 발생시키고, 최종 재고 정합성을 확인하는 방식.

## 4. 테스트 데이터 전략

### 기본 원칙
- 테스트는 서로 독립적이어야 하며 실행 순서에 의존하지 않는다.
- 테스트에서 필요한 데이터만 최소 생성한다.
- 식별자(ID) 하드코딩을 피하고, 생성 결과에서 동적으로 참조한다.

### 데이터 생성 방법

| 데이터      | 생성 방법                           | 이유                                      |
|----------|---------------------------------|-----------------------------------------|
| Category | `POST /api/categories` API 호출   | 공개 API 존재. 실제 사용자 흐름 재현.                |
| Product  | `POST /api/products` API 호출     | 공개 API 존재. 실제 사용자 흐름 재현.                |
| Option   | `OptionRepository.save()` 직접 호출 | 공개 API 없음. `@Autowired`로 주입.            |
| Member   | `MemberRepository.save()` 직접 호출 | 공개 API 없음. sender/receiver 모두 사전 생성 필요. |

> **주의**: `FakeGiftDelivery.deliver()`에서 `memberRepository.findById(gift.getFrom()).orElseThrow()`를 호출하므로, sender Member가 DB에 반드시 존재해야 한다.

### 선물 테스트의 Given 순서

```
1. Member 생성 (sender, receiver) — Repository
2. Category 생성                  — API
3. Product 생성                   — API
4. Option 생성 (초기 재고 지정)     — Repository
```

### 구체적인 셋업 코드

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    Long senderId;
    Long receiverId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        var sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        var receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
        senderId = sender.getId();
        receiverId = receiver.getId();
    }
}
```

### DB 초기화 방식

**매 테스트마다 TRUNCATE한다** (확정).

```java
@BeforeEach
void setUp() {
    RestAssured.port = port;
    // 순서 주의: FK 제약 조건 때문에 자식 테이블부터 삭제
    optionRepository.deleteAllInBatch();
    productRepository.deleteAllInBatch();
    memberRepository.deleteAllInBatch();
    categoryRepository.deleteAllInBatch();
    // 이후 테스트 데이터 생성
}
```

- `deleteAllInBatch()`는 벌크 DELETE로 빠르게 동작한다.
- FK 제약 조건 순서를 지켜야 하므로 자식 → 부모 순서로 삭제한다.
- H2 in-memory를 사용하되, `IDENTITY` 전략으로 인해 ID가 계속 증가하므로 ID 값에 의존하지 않는다.

## 5. 검증 전략

### 무엇을 검증할 것인가

- **HTTP 관점**: 상태 코드(성공/실패), 응답 바디의 필드 존재 및 값
- **도메인 관점**: 선물 성공 후 `Option.quantity` 감소 여부, 선물 실패 시 재고 불변(트랜잭션 롤백)
- **영속성 관점**: 생성 API 호출 후 조회 API에서 실제 저장 상태 확인

### 어떻게 검증할 것인가

- **API 호출**: RestAssured를 사용한다 (MockMvc는 사용하지 않는다).
- **상태 확인**: API 응답 확인 + Repository 재조회로 DB 상태까지 확인한다.
- **실패 검증**: 상태코드만 보지 말고 "실패 후 상태 불변"까지 함께 확인한다.

```java
// 실패 검증 예시: 재고 부족 시 재고 불변 확인
var before = optionRepository.findById(optionId).get().getQuantity();
var response = 선물_전달(optionId, before + 1, receiverId, "선물", senderId);
assertThat(response.statusCode()).isEqualTo(400);

var after = optionRepository.findById(optionId).get().getQuantity();
assertThat(after).isEqualTo(before);  // 재고 불변 확인
```

### 시나리오 템플릿 (Given-When-Then)
1. Given: 초기 재고가 10인 옵션이 있다.
2. When: 수량 3으로 선물을 요청한다.
3. Then: 응답 성공, 옵션 재고는 7이다.

## 6. 주요 의사결정

| 번호 | 결정                                       | 이유                                                                           |
|----|------------------------------------------|------------------------------------------------------------------------------|
| 1  | 인수 테스트의 1순위를 선물하기에 둔다.                   | 도메인 핵심이며 재고 오류의 비용이 가장 크다.                                                   |
| 2  | Option/Member는 Repository로 직접 생성한다.      | 공개 API가 없다. `OptionRepository`, `MemberRepository`를 `@Autowired`로 주입하여 사용한다. |
| 3  | API 호출은 RestAssured만 사용한다.               | CLAUDE.md 가이드에 따라 통일한다. MockMvc는 사용하지 않는다.                                   |
| 4  | 매 테스트마다 `deleteAllInBatch()`로 DB를 초기화한다. | 테스트 독립성 보장. `@Sql` TRUNCATE보다 코드 레벨에서 제어가 명확하다.                              |
| 5  | 성공/실패 모두 DB 최종 상태를 검증한다.                 | 인수 테스트 목적은 응답 확인이 아니라 시스템 행위 보장이다.                                           |
| 6  | 초기 단계에서는 동시성보다 단일 트랜잭션 정합성부터 고정한다.       | 기본 규칙이 안정화되어야 동시성 정책 검증 의미가 생긴다.                                             |
