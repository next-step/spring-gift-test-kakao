# 테스트 전략

## 목적
Spring Boot 선물/상품 관리 시스템의 External Behavior(외부 행위)를 검증하는 인수 테스트를 통해 리팩터링 안전망을 구축한다.

## 페어 논의 기록

### 이 시스템의 핵심 기능은 무엇인가

도메인 의존 관계가 Category → Product → Option → Gift 순서로 흘러가므로, 이 시스템의 최종 목적은 선물 전송이다. `GiftService.give()`가 옵션 재고를 차감하고 배송을 수행하는 핵심 플로우이며, 카테고리와 상품 CRUD는 선물을 보내기 위한 사전 데이터 준비 역할이다. Wish 기능은 서비스 계층만 존재하고 컨트롤러가 없어서 현재 HTTP로 접근할 수 없다.

### 테스트 대상을 선정하면서 가장 고민했던 부분

재고 차감의 검증 범위가 가장 논쟁적이었다. HTTP 응답 상태 코드만 확인하면 되는지, 아니면 DB까지 열어봐야 하는지가 쟁점이었다. 결론적으로 HTTP 응답만으로는 트랜잭션 롤백 여부를 알 수 없기 때문에, 실패 시나리오에서는 응답 코드 확인과 함께 DB에서 재고가 변경되지 않았는지까지 검증하기로 했다. 예를 들어 재고 부족으로 500이 돌아왔을 때, 실제로 재고가 줄지 않았는지를 `OptionRepository`로 확인한다.

카테고리/상품 CRUD는 단순해서 테스트할 가치가 있는지 잠깐 고민했지만, 리팩터링 중 JPA 매핑이 깨지면 전체 기능이 무너지므로 기본 조회 검증은 포함하기로 했다. 참조 무결성 에러(없는 카테고리로 상품 생성, 없는 옵션으로 선물 전송)도 실패가 조용히 무시되면 안 되므로 포함했다.

### 성공과 실패 시나리오의 균형

성공 시나리오는 수동으로도 쉽게 확인할 수 있지만, 실패 시나리오는 놓치기 쉽다는 점에서 오히려 실패 케이스가 더 중요하다는 의견이 나왔다. 다만 현재 글로벌 예외 핸들러가 없어서 모든 에러가 500으로 나오는 상황이라, 에러 응답 본문으로 실패 원인을 구분할 수 없다. 일단 상태 코드 500으로 검증하고, 예외 핸들러가 추가되면 400이나 409 같은 구체적인 코드로 테스트를 업데이트하면 된다고 정리했다. 최종적으로 성공 3개, 실패 3개로 균형을 맞췄다.

### 테스트 범위에서 제외한 것

Wish, Option, Member는 REST 컨트롤러가 없어서 HTTP 경계에서 접근 자체가 불가능하다. `FakeGiftDelivery`는 콘솔 출력만 하는 가짜 구현이라 배송 결과를 검증할 방법이 없고, 실제 카카오 API로 교체되면 그때 Mock 도입을 검토하면 된다. 동시성 문제(같은 옵션에 동시 요청 시 재고 음수 가능성)도 논의했지만, 현재 코드에 락이 없어서 이건 구현의 한계이지 테스트로 잡을 문제가 아니라고 판단했다.

---

## 검증 행위 목록

| # | 행위 | 엔드포인트 | 검증 이유 |
|---|------|-----------|----------|
| 1 | 선물을 보내면 옵션 재고가 감소한다 | POST /api/gifts | 매출/재고 핵심 로직, 트랜잭션 경계 |
| 2 | 재고 부족 시 선물 전송이 실패한다 | POST /api/gifts | 에러 계약, 비즈니스 무결성 |
| 3 | 카테고리를 생성하면 조회 시 포함된다 | GET /api/categories | 상품 등록의 전제조건 |
| 4 | 상품을 생성하면 조회 시 포함된다 | GET /api/products | 핵심 CRUD, 카테고리 연관 검증 |
| 5 | 존재하지 않는 카테고리로 상품 생성 시 실패 | POST /api/products | 데이터 무결성 |
| 6 | 존재하지 않는 옵션으로 선물 전송 시 실패 | POST /api/gifts | 참조 무결성 |

## 데이터 전략

- **`@Sql` 애노테이션**: 테스트 데이터를 SQL 스크립트로 선언적으로 관리
  - `cleanup.sql`: `TRUNCATE TABLE` + `SET REFERENTIAL_INTEGRITY FALSE`로 전체 데이터 초기화
  - 테스트 클래스별 데이터 파일: `category-data.sql`, `product-data.sql`, `gift-data.sql`
  - 각 테스트 메서드 실행 전 cleanup → data 순서로 실행 (BEFORE_TEST_METHOD)
- **H2 인스턴스 재활용**: 고정 URL(`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`)로 모든 테스트가 동일한 H2 인스턴스 공유
- **테스트 격리**: `@Sql` cleanup 스크립트가 매 테스트 전 TRUNCATE 수행하여 격리 보장
- **DB 상태 검증**: 선물 전송 후 재고 변경 확인 시 `OptionRepository`로 직접 조회

## 검증 전략

- **HTTP 응답 검증**: 상태 코드 + 응답 바디 (RestAssured + Hamcrest)
- **DB 상태 검증**: Repository 직접 조회 (AssertJ)
- **조합 검증**: 선물 전송 시 HTTP 200 확인 후 DB에서 재고 감소 확인

## 기술 스택

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + RestAssured
- `@Sql` 애노테이션으로 선언적 테스트 데이터 관리
- `@Transactional` 미사용 (실제 HTTP 요청은 별도 스레드에서 실행)
- JUnit 5 + Hamcrest + AssertJ

## 주요 의사결정

1. **RestAssured 선택 이유**: HTTP 경계에서의 행위 검증에 적합한 DSL 제공
2. **`@Sql` 기반 데이터 관리**: Repository 직접 호출 대신 SQL 스크립트로 선언적 데이터 설정. 테스트 코드에서 데이터 준비 로직을 분리하여 가독성 향상
3. **Category/Product POST 바인딩 이슈**: `CategoryRestController`와 `ProductRestController`는 `@RequestBody` 없이 `@ModelAttribute` 바인딩을 사용하나, DTO 클래스에 setter가 없어 필드 값이 null로 바인딩됨. 따라서 Category/Product 데이터는 Repository로 직접 생성하고 GET 엔드포인트만 HTTP로 검증
4. **GiftRestController**: `@RequestBody`를 사용하므로 Jackson JSON 역직렬화가 정상 동작하여 POST 엔드포인트 직접 검증 가능
5. **에러 응답 코드**: 현재 글로벌 예외 핸들러가 없으므로 서버 에러(500) 반환 검증
