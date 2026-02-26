# 선물하기 시스템 테스트 전략

## 1. 검증할 행위 목록

### 선택한 10가지 핵심 행위

| # | 행위 | 선택 기준 |
|---|------|----------|
| 1 | **선물하기 시 재고 감소** | 핵심 비즈니스 로직 - 선물의 본질적 기능 |
| 2 | **누적 재고 감소** | 상태 일관성 - 연속 작업 시 정확성 검증 |
| 3 | **재고 부족 시 실패** | 비즈니스 규칙 - 재고보다 많이 선물 불가 |
| 4 | **재고 소진 후 실패** | 경계값 - 재고 0일 때 추가 선물 불가 |
| 5 | **존재하지 않는 옵션으로 실패** | 예외 처리 - 잘못된 입력 대응 |
| 6 | **재고와 정확히 같은 수량 (경계값)** | 경계값 - stock == quantity 검증 |
| 7 | **수량 0으로 선물 시도** | 무효 입력 - 0 수량 처리 검증 |
| 8 | **음수 수량으로 선물 시도** | 입력 검증 - 음수 수량 방어 로직 검증 |
| 9 | **존재하지 않는 발신자로 실패** | 예외 처리 - 잘못된 Member-Id 대응 |
| 10 | **실패 후 정상 선물로 불변성 검증** | 트랜잭션 무결성 - 롤백 후 재고 정합성 |

### 선택 기준

```
우선순위 매트릭스:
┌─────────────────┬──────────────┬──────────────┐
│                 │ 영향도 높음   │ 영향도 낮음   │
├─────────────────┼──────────────┼──────────────┤
│ 발생 빈도 높음   │ ① 재고 감소  │              │
│                 │ ② 누적 감소  │              │
├─────────────────┼──────────────┼──────────────┤
│ 발생 빈도 낮음   │ ③ 재고 부족  │ 동시성 이슈   │
│                 │ ④ 재고 소진  │ 네트워크 오류  │
│                 │ ⑤ 잘못된 옵션 │              │
└─────────────────┴──────────────┴──────────────┘
```

**선택 원칙:**
1. **비즈니스 크리티컬**: 재고 관리는 선물하기의 핵심
2. **경계값 테스트**: 0, 부족, 소진 등 엣지 케이스 포함
3. **실패 시나리오**: 성공뿐 아니라 실패도 검증

---

## 2. 테스트 데이터 전략

### 데이터 준비 방식

```java
// Fixture Factory 패턴
private Option createOptionWithStock(int stock) {
    Category category = categoryRepository.save(new Category("테스트 카테고리"));
    Product product = productRepository.save(
        new Product("테스트 상품", 10000, "http://test.jpg", category)
    );
    return optionRepository.save(new Option("테스트 옵션", stock, product));
}
```

### 데이터 격리 전략

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)  // PostgreSQL Testcontainer
class GiftAcceptanceTest {

    @BeforeEach
    void setUp() {
        // TRUNCATE로 빠른 데이터 초기화 + RESTART IDENTITY로 ID 시퀀스 리셋
        jdbcTemplate.execute("TRUNCATE TABLE wish, option, product, category, member RESTART IDENTITY CASCADE");
    }
}
```

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| `@DirtiesContext` | 테스트 후 컨텍스트 재생성 | 완벽한 격리 | 느린 속도 (10회 컨텍스트 재시작) |
| `@Transactional` | 테스트 후 롤백 | 빠름 | TestRestTemplate과 별도 스레드로 동작 불가 |
| **TRUNCATE CASCADE** | 테이블 데이터 초기화 | 빠름 + 완벽한 격리 | Docker 필요 (Testcontainers) |
| **선택: TRUNCATE + Testcontainers** | 프로덕션 DB(PostgreSQL)와 동일 환경 + 빠른 격리 |

### 테스트 데이터 수치

```
재고 10 → 3개 선물 → 재고 7 검증 (행위 1)
재고 20 → 5개 + 7개 선물 → 재고 8 검증 (행위 2)
재고 5 → 10개 선물 시도 → 실패, 재고 5 유지 (행위 3)
재고 3 → 3개 선물 → 재고 0 → 1개 추가 시도 → 실패 (행위 4)
```

---

## 3. 검증 전략

### 검증 레이어

```
┌─────────────────────────────────────────┐
│           HTTP Response 검증            │  ← API 레벨
│  - 상태 코드 (200 OK, 5xx Error)        │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           데이터 상태 검증               │  ← DB 레벨
│  - 재고 수량 변화                        │
│  - 데이터 일관성                         │
└─────────────────────────────────────────┘
```

### 검증 코드 패턴

```java
// 성공 케이스: 상태 코드 + 데이터 변경 모두 검증
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(getStock(option.getId())).isEqualTo(7);

// 실패 케이스: 에러 응답 + 데이터 무결성 검증
assertThat(response.getStatusCode().is5xxServerError()).isTrue();
assertThat(getStock(option.getId())).isEqualTo(5);  // 변경 없음
```

### Given-When-Then 구조

```java
@Test
@DisplayName("선물하면 옵션의 재고가 요청한 수량만큼 감소한다")
void decreasesStockByRequestedQuantity() {
    // given - 테스트 사전 조건
    Option option = createOptionWithStock(10);

    // when - 테스트 대상 행위 실행
    ResponseEntity<Void> response = sendGift(option.getId(), 3);

    // then - 결과 검증
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getStock(option.getId())).isEqualTo(7);
}
```

---

## 4. 주요 의사결정

### 결정 1: 테스트 범위

| 선택지 | 설명 | 결정 |
|--------|------|------|
| 단위 테스트 | Service/Domain 레벨 Mock 테스트 | ❌ |
| 통합 테스트 | Repository + Service 테스트 | ❌ |
| **인수 테스트** | HTTP API 엔드투엔드 테스트 | ✅ |

**이유:** 실제 사용자 관점에서 전체 흐름을 검증하고, 컴포넌트 간 연동 문제를 발견하기 위함

### 결정 2: 테스트 구조

| 선택지 | 설명 | 결정 |
|--------|------|------|
| 메서드별 그룹화 | `testGive()`, `testDecrease()` | ❌ |
| **행위별 그룹화** | `@Nested` 클래스로 행위 단위 | ✅ |

**이유:** 행위 중심 테스트가 요구사항과 1:1 매핑되어 가독성 향상

```java
@Nested
@DisplayName("행위 1: 선물하기 시 재고 감소")
class GiftDecreasesStock { ... }
```

### 결정 3: API 호출 방식

| 선택지 | 설명 | 결정 |
|--------|------|------|
| MockMvc | 컨트롤러만 테스트 | ❌ |
| **TestRestTemplate** | 실제 HTTP 요청 | ✅ |

**이유:** 실제 서버를 띄우고 HTTP 통신하여 직렬화/역직렬화, 필터, 인터셉터 등 전체 스택 검증

### 결정 4: 데이터 정리 방식

| 선택지 | 장점 | 단점 | 결정 |
|--------|------|------|------|
| `@Transactional` 롤백 | 빠름 | TestRestTemplate 별도 스레드 → 동작 불가 | ❌ |
| `@DirtiesContext` | 완벽한 격리 | 컨텍스트 재시작 → 느림 (60-90s) | ❌ |
| **TRUNCATE + Testcontainers** | 빠름 + 프로덕션 DB 동일 | Docker 필요 | ✅ |

**이유:** PostgreSQL Testcontainer로 프로덕션과 동일한 DB를 사용하면서, TRUNCATE CASCADE로 빠른 격리 달성 (0.567s / 17 tests)

### 결정 5: 예외 검증 방식

| 선택지 | 설명 | 결정 |
|--------|------|------|
| 예외 타입 검증 | `assertThrows(IllegalStateException.class)` | ❌ |
| **HTTP 상태 코드 검증** | `is5xxServerError()` | ✅ |

**이유:** 인수 테스트는 내부 구현(예외 타입)이 아닌 외부 동작(HTTP 응답)을 검증해야 함

---

## 5. 테스트 실행

```bash
# 전체 테스트
./gradlew test --tests "gift.GiftAcceptanceTest"

# 특정 행위만
./gradlew test --tests "gift.GiftAcceptanceTest\$GiftDecreasesStock"
```

## 6. 테스트 결과

```
🎁 선물하기 테스트 결과

✅ PASSED 행위 1: 선물하기 시 재고 감소
✅ PASSED 행위 2: 누적 재고 감소
✅ PASSED 행위 3: 재고 부족 시 실패
✅ PASSED 행위 4: 재고 소진 후 실패
✅ PASSED 행위 5: 존재하지 않는 옵션으로 실패
✅ PASSED 행위 6: 재고와 정확히 같은 수량 (경계값)
✅ PASSED 행위 7: 수량 0으로 선물 시도
✅ PASSED 행위 8: 음수 수량으로 선물 시도 (입력 검증)
✅ PASSED 행위 9: 존재하지 않는 발신자로 실패
✅ PASSED 행위 10: 실패 후 정상 선물로 불변성 검증

결과: 10/10 통과 (인수 테스트)
결과: 7/7 통과 (단위 테스트 - OptionTest)
```

## 7. GAN 방식 테스트 개선 과정

### 라운드 1 (불합격)
- **Generator 제안**: 단위 테스트 추가, 경계값/음수/발신자 검증 필요
- **Critic 비평**: 점수 6/10, 5개 합격 조건 미충족

### 라운드 2 (합격)
- **Generator 평가**: 점수 8/10, 합의 가능
- **Critic 재비평**: 점수 9/10, 합격

### 라운드 3 (최종 합의)
- **추가 개선**: OptionTest 음수 입력 테스트, 행위 2 중간 응답 검증
- **Generator 평가**: 점수 9/10, 합의 가능
- **Critic 최종 판정**: 점수 9/10, **합격**
- **감점 이유**: HTTP 상태 코드 의미론적 구분 불가 (프로덕션 코드 한계)

## 8. 최종 테스트 현황

| 구분 | 테스트 수 | 실행 방법 | 상태 |
|------|----------|-----------|------|
| 단위 테스트 (OptionTest) | 7개 | `./gradlew test` | ✅ 통과 |
| 인수 테스트 (GiftAcceptanceTest) | 10개 | `./gradlew test` | ✅ 통과 |
| Cucumber BDD (gift.feature) | 7개 | `./gradlew cucumberTest` | ✅ 통과 |
| **총 테스트** | **24개** | `./gradlew test cucumberTest` | ✅ 전체 통과 |

### 최종 점수: 9/10 (합격)

---

## 9. Cucumber BDD 테스트 체계

### 9.1 아키텍처

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Test JVM    │     │  App Container   │     │  PostgreSQL      │
│  (Cucumber)  │────▶│  (port 28080)    │────▶│  (port 15432)    │
│  RestAssured │     │  Spring Boot     │     │  postgres:16     │
│  JdbcTemplate│─────┼──────────────────┼────▶│                  │
└──────────────┘     └──────────────────┘     └──────────────────┘
   HTTP 요청 ─────▶      /api/gifts              DB (공유)
   DB 직접 접근 ────────────────────────────▶  TRUNCATE/INSERT/SELECT
```

### 9.2 Korean Gherkin 시나리오

```gherkin
# language: ko
기능: 선물하기

  시나리오: 선물하면 재고가 감소한다
    조건 재고가 10인 옵션이 존재한다
    만일 3개를 선물한다
    그러면 응답 상태코드는 200이다
    그리고 재고는 7이다
```

### 9.3 실행 명령어

```bash
./gradlew test              # 17개 JUnit 테스트 (Testcontainers)
./gradlew cucumberTest      # 7개 Cucumber 시나리오 (Docker Compose + 컨테이너화된 앱)
./gradlew test cucumberTest # 전체 24개 테스트
```

### 9.4 Docker Compose 구성

| 서비스 | 이미지 | 포트 | 용도 |
|--------|--------|------|------|
| postgres | postgres:16-alpine | 15432 | 테스트 데이터베이스 |
| app | Dockerfile (multi-stage) | 28080 | 컨테이너화된 Spring Boot |

### 9.5 주요 설계 결정

| 결정 | 선택 | 이유 |
|------|------|------|
| HTTP 클라이언트 | RestAssured | @SpringBootTest가 다른 클래스에 있어도 독립 동작 |
| DB 검증 | JdbcTemplate | Hibernate 캐시 우회, 컨테이너와 별도 세션 |
| 테스트 격리 | TRUNCATE CASCADE | 시나리오 간 데이터 독립 보장 |
| 앱 실행 환경 | webEnvironment=NONE | 앱은 Docker 컨테이너에서 실행 |
| 프로필 | cucumber | 기존 JUnit 테스트와 설정 분리 |
