# 테스트 전략 문서

## 1. 검증할 행위 목록

### 선정 기준
"이 시스템이 사용자에게 보장해야 하는 결과는 무엇인가?"를 기준으로 선정했습니다.
`verify(option).decrease(3)` 같은 **구현 호출 여부**가 아니라, **비즈니스 결과**를 기준으로 삼았습니다.

### 행위 목록

| # | 행위 | 파일 | 선정 이유 |
|---|------|------|-----------|
| 1 | 카테고리를 생성하면 목록에서 확인할 수 있다 | `CategoryAcceptanceTest` | 가장 기본적인 CRUD. "다음 행동(GET)으로 이전 행동(POST)을 검증" 패턴 적용 |
| 2 | 여러 카테고리를 생성하면 모두 목록에 나타난다 | `CategoryAcceptanceTest` | 복수 데이터가 올바르게 저장·조회되는지 검증 |
| 3 | 유효한 카테고리로 상품을 생성하면 목록에서 확인할 수 있다 | `ProductAcceptanceTest` | 연관관계(Category→Product)를 가로지르는 생성 행위 |
| 4 | 여러 상품을 생성하면 모두 목록에 나타난다 | `ProductAcceptanceTest` | 복수 상품의 독립적 저장 보장 |
| 5 | 재고가 충분하면 선물을 보낼 수 있다 | `GiftAcceptanceTest` | 정상 경로 보장. 선물 API의 기본 동작 |
| 6 | 선물을 보내면 재고가 차감된다 | `GiftAcceptanceTest` | **핵심 행위.** 재고 차감이라는 상태 변화를 증명 |
| 7 | 재고보다 많은 수량을 선물하면 실패한다 | `GiftAcceptanceTest` | 재고 보호 규칙이 API 경계까지 관통하는지 검증 |
| 8 | 재고를 전부 소진하면 더 이상 선물할 수 없다 | `GiftAcceptanceTest` | 누적 차감이 올바르게 영속화되는지 검증 (트랜잭션 경계 검증) |

---

## 2. 테스트 데이터 전략

### 결정: REST API 전용 준비

테스트 데이터를 준비할 때 두 가지 선택지가 있었습니다.

**선택지 A: REST API 호출로 준비**
```java
// API를 통해 데이터 삽입
Long categoryId = given().body(...).post("/api/categories").then().extract().jsonPath().getLong("id");
```

**선택지 B: @Autowired Repository 직접 주입**
```java
// DB에 직접 삽입
@Autowired CategoryRepository categoryRepository;
categoryRepository.save(new Category("전자기기"));
```

**A를 선택한 근거:**

> 보호 대상은 `decrease()` 호출이 아니라 **재고 감소 결과**이다.

테스트도 같은 원칙을 따라야 합니다. 테스트 준비(Given) 단계에서 Repository를 직접 사용하면, 테스트가 영속성 레이어의 구현 세부사항(JPA Entity 생성자, 연관관계 매핑 방식 등)에 결합됩니다. 나중에 영속성 구현이 바뀌면 테스트 setUp도 함께 깨집니다.

반면 API로 준비하면:
- 테스트 준비 코드 자체가 "카테고리 생성 API가 동작한다"는 행위를 전제로 하므로, 인수 테스트끼리 서로를 간접 검증합니다.
- 영속성 구현이 바뀌어도 API 계약이 유지되는 한 테스트는 깨지지 않습니다.

**Member, Option REST 엔드포인트 추가:**

`/api/members`, `/api/options`는 기존에 서비스 레이어가 이미 완성되어 있었지만 REST 컨트롤러가 없었습니다. 이를 **테스트를 위해** 추가한 것이 아니라, 자연스럽게 완성해야 할 CRUD 레이어를 인수 테스트가 먼저 요구하는 형태로 작성했습니다. 테스트가 시스템의 첫 번째 사용자입니다.

### 테스트 격리 방법

`@SpringBootTest(webEnvironment = RANDOM_PORT)` + H2 인메모리 DB 사용.

각 테스트 전 `@BeforeEach`에서 H2의 `SET REFERENTIAL_INTEGRITY FALSE` + `TRUNCATE TABLE`로 모든 테이블을 초기화합니다.

`@DirtiesContext`나 `@Transactional`을 사용하지 않은 이유:
- `@DirtiesContext`: 테스트마다 Spring Context를 재시작 → 매우 느림
- `@Transactional`: 실제 서버가 별도 스레드에서 실행되므로 테스트 트랜잭션이 서버 트랜잭션을 롤백하지 못함

---

## 3. 검증 전략

### 핵심 원칙: "다음 행동으로 이전 행동을 검증한다"

```
POST /api/categories → 응답 코드만으로는 부족
GET  /api/categories → 생성된 데이터가 실제로 존재함을 증명
```

### 상태 변화 검증: DB 직접 조회 없이

재고 차감이라는 상태 변화를 `option.getQuantity()`를 직접 읽지 않고 검증하는 방법:

```
재고 1개짜리 옵션 생성
  ↓
선물 1개 전송 (성공 확인)
  ↓
선물 1개 재시도 → 500 에러
  ↓
결론: 첫 번째 선물이 실제로 재고를 차감해서 DB에 반영했다
```

이것이 핵심입니다. `verify(option).decrease(1)` 같은 단위 테스트 검증은 다음을 보호하지 못합니다:
- 트랜잭션이 실제로 커밋되었는가?
- `flush()`가 호출되어 DB에 반영되었는가?
- 영속성 컨텍스트가 올바르게 동작하는가?

인수 테스트는 이 모든 경로가 제대로 동작함을 한 번에 보호합니다.

---

## 4. 주요 의사결정

### 결정 1: 실패 응답 코드를 500으로 검증

`GiftService`에서 재고 부족 시 `IllegalStateException`을 던집니다. 현재 예외 처리 로직이 없어서 Spring이 500을 반환합니다.

이를 400이나 422로 바꾸는 것이 더 적절하지만, 그 변경은 이번 미션 범위 밖입니다. 현재 시스템의 실제 동작을 검증하는 것이 인수 테스트의 역할이므로, 500을 그대로 기대값으로 사용합니다. (추후 예외 처리를 추가하면 테스트 기대값도 함께 바뀌어야 한다는 것을 문서화합니다.)

### 결정 2: Wish 행위를 포함하지 않은 이유

`WishService`가 존재하지만, `POST /api/wishes` 엔드포인트가 없었습니다. 위시리스트 기능은 현재 시스템에서 사용자에게 노출되지 않는 미완성 기능이므로 이번 인수 테스트 범위에서 제외했습니다.