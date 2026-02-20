# 인수 테스트 전략

## 1. 검증할 행위 목록

### 선택 기준: 클라이언트의 행위에 중점

인수 테스트는 **사용자(클라이언트)가 시스템과 상호작용하는 행위**를 검증합니다. 내부 구현이 아닌 외부에서 관찰 가능한 동작에 집중합니다.

| 행위 | API | 선택 이유 |
|------|-----|-----------|
| 카테고리 생성 | `POST /api/categories` | 사용자가 새 카테고리를 등록하는 행위 |
| 카테고리 목록 조회 | `GET /api/categories` | 사용자가 카테고리를 탐색하는 행위 |
| 상품 등록 | `POST /api/products` | 사용자가 새 상품을 등록하는 행위 |
| 상품 목록 조회 | `GET /api/products` | 사용자가 상품을 탐색하는 행위 |
| 선물 보내기 | `POST /api/gifts` | 사용자가 다른 회원에게 선물하는 핵심 행위 |

---

## 2. 테스트 데이터 전략

### 2.1 준비 (Setup) - `@BeforeEach`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
```

| 설정 | 설명 |
|------|------|
| `RANDOM_PORT` | 실제 서버 구동, 포트 충돌 방지 |
| `RestAssured.port` | 매 테스트 전 동적 포트 바인딩 |
| 사전 데이터 | 테스트에 필요한 엔티티를 Repository로 직접 저장 |

### 2.2 정리 (Teardown) - `@AfterEach`

```java
@AfterEach
void tearDown() {
    categoryRepository.deleteAllInBatch();
}
```

| 전략 | 설명 |
|------|------|
| **`@Transactional` 미사용** | RestAssured는 별도 스레드에서 HTTP 요청 → 테스트 트랜잭션과 분리됨 (트랜잭션이 이미 커밋됨) |
| **명시적 삭제** | `deleteAllInBatch()`로 테스트 데이터 직접 삭제 |
| **삭제 순서** | 외래키 제약 시 자식 → 부모 순서로 삭제 |

### 2.3 왜 `@Transactional` 롤백이 안 되는가?

```
[테스트 스레드]                    [서버 스레드]
     │                                │
     │  ── HTTP 요청 ──────────────▶   │
     │                                │ DB INSERT (별도 트랜잭션)
     │  ◀─────────── HTTP 응답 ───     │
     │                                │
 @Transactional 롤백               이미 커밋됨
     (효과 없음)
```

---

## 3. 검증 전략

### 보호하려는 것: 외부(사용자)의 행동

시스템 경계(API)에서 **사용자 시나리오를 최종 상태 기준으로 검증**합니다.

### 3.1 시나리오별 검증 항목

| 검증 항목 | 설명 | 예시 |
|-----------|------|------|
| **HTTP 상태 코드** | 요청 성공/실패 여부 | `statusCode(200)`, `statusCode(400)` |
| **응답 바디 구조** | 반환 데이터 형식과 값 | `body("id", notNullValue())` |
| **상태 변화** | 요청 후 시스템 상태 변경 확인 | 재고 10 → 7 (DB 조회) |
| **경계값** | 극단적 입력에서의 동작 | 재고와 동일한 수량 요청 → 재고 0 |
| **비즈니스 규칙** | 단순 메서드 호출이 아닌 결과 검증 | 재고 부족 시 500 에러 + 재고 변화 없음 |

### 3.2 검증 예시

```java
// HTTP 상태 코드 + 응답 바디
.then()
    .statusCode(200)
    .body("id", notNullValue())
    .body("name", equalTo("식품"));

// 상태 변화 (DB 조회)
Option updated = optionRepository.findById(option.getId()).orElseThrow();
assertThat(updated.getQuantity()).isEqualTo(7);

// 비즈니스 규칙 위반 시 롤백 확인
.then()
    .statusCode(500);
Option unchanged = optionRepository.findById(option.getId()).orElseThrow();
assertThat(unchanged.getQuantity()).isEqualTo(10);  // 변화 없음
```

---

## 4. 주요 의사결정

### MockMVC vs RestAssured

| 항목 | MockMVC | RestAssured |
|------|---------|-------------|
| **서버 실행** | X (서블릿 컨테이너 모킹) | O (실제 서버 구동) |
| **테스트 환경** | Spring 컨텍스트만 | 운영 환경과 유사 |
| **HTTP 계약** | 부분 검증 | 완전 검증 |
| **속도** | 빠름 | 상대적으로 느림 |

### 선택: RestAssured

**이유:**
1. **인수 테스트의 목적** - "사용자 행위(HTTP 계약이 지켜지는지)"를 검증
2. **환경 유사성** - 실제 서버가 구동되어 운영 환경과 비슷
3. **현실성** - MockMVC는 인수 테스트의 강도(현실성)가 떨어질 수 있음

```java
// RestAssured: 실제 HTTP 요청/응답
given()
    .contentType(ContentType.JSON)
    .body(Map.of("name", "식품"))
.when()
    .post("/api/categories")
.then()
    .statusCode(200)
    .body("name", equalTo("식품"));
```

---

## 5. 테스트 클래스 구조

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private GiftDelivery giftDelivery;  // 외부 의존성 격리

    @Autowired
    private OptionRepository optionRepository;
    // ... 기타 Repository

    private Option option;
    private Member sender;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // 사전 데이터 생성 (부모 → 자식 순서)
    }

    @AfterEach
    void tearDown() {
        // 데이터 정리 (자식 → 부모 순서)
    }

    @Test
    @DisplayName("유효한 요청으로 선물을 보내면 200 OK와 재고가 차감된다")
    void give_validRequest_returnsOkAndDecreasesStock() {
        // given: @BeforeEach에서 준비됨

        // when
        given()
            .contentType(ContentType.JSON)
            .header("Member-Id", sender.getId())
            .body(Map.of(...))
        .when()
            .post("/api/gifts")
        .then()
            .statusCode(200);

        // then: 상태 변화 검증
        Option updated = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(7);
    }
}
```

---

## 6. 체크리스트

### 테스트 작성 전

- [ ] 검증할 사용자 행위 식별
- [ ] API 엔드포인트와 요청 형식 확인
- [ ] 비즈니스 규칙 파악 (Service의 if/throw 패턴)
- [ ] 엔티티 의존 관계 파악

### 테스트 작성 시

- [ ] `@BeforeEach`: 포트 설정 + 사전 데이터 생성
- [ ] `@AfterEach`: 역순 삭제
- [ ] 외부 의존성 `@MockitoBean` 처리
- [ ] 정상/실패 시나리오 모두 작성
- [ ] **최종 상태 기준 검증** (HTTP 응답 + DB 상태)
