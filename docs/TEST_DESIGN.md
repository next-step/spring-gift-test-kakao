# 인수 테스트 설계

## 단위 테스트 vs 인수 테스트

### 단위 테스트 (Unit Test)

단위 테스트는 **코드 안쪽에서 바깥을 보는 시선**이다.

```
테스트 → [메서드 하나] → 결과 확인
```

- 대상: 클래스, 메서드 하나
- 질문: "이 메서드가 올바르게 동작하는가?"
- 의존성: 없거나 mock으로 대체
- 예시: `Option.decrease(30)` 호출 후 quantity가 70인지 확인

단위 테스트는 **구현을 알고 있는 개발자**가 작성한다. 내부 구조가 바뀌면 테스트도 바뀐다.

### 인수 테스트 (Acceptance Test)

인수 테스트는 **시스템 바깥에서 안쪽을 보는 시선**이다.

```
테스트 → [HTTP 요청] → 서버 전체 → [HTTP 응답 + DB 상태] 확인
```

- 대상: 사용자가 경험하는 기능 전체
- 질문: "사용자가 이 기능을 사용하면 기대한 결과를 얻는가?"
- 의존성: 실제 서버, 실제 DB, 실제 HTTP
- 예시: 선물하기 API를 호출하면 응답이 200이고, DB에서 재고가 줄어 있는가

인수 테스트는 **내부 구현을 모른다고 가정**한다. 컨트롤러가 어떤 서비스를 호출하는지, 트랜잭션이 어떻게 걸리는지 신경 쓰지 않는다. 오직 **요청과 응답, 그리고 관찰 가능한 부수 효과**(DB 상태 변화 등)만 검증한다.

### 핵심 차이

| | 단위 테스트 | 인수 테스트 |
|---|-----------|-----------|
| 시점 | 코드 내부 | 시스템 외부 |
| 설계 출발점 | 클래스, 메서드 | 사용자 시나리오 |
| 깨지는 이유 | 구현이 바뀔 때 | 기능이 바뀔 때 |
| 속도 | 빠름 (ms) | 느림 (서버 기동 필요) |
| 신뢰도 | 부분적 (조합은 검증 못함) | 높음 (실제 동작 전체 검증) |

**설계 순서가 다르다.** 단위 테스트는 "어떤 클래스의 어떤 메서드를 테스트할까?"에서 시작하지만, 인수 테스트는 **"사용자가 무엇을 할 수 있어야 하는가?"**에서 시작한다.

---

## 인수 테스트 설계

### 설계 원칙

1. **시나리오에서 출발한다** — 코드가 아니라, 사용자가 수행하는 행위를 먼저 정의한다
2. **블랙박스로 검증한다** — 내부 구현(어떤 서비스, 어떤 쿼리)에 의존하지 않는다
3. **관찰 가능한 결과만 단언한다** — HTTP 응답, DB 상태 변화
4. **각 시나리오는 독립적이다** — 다른 테스트의 결과에 의존하지 않는다

### Feature 1: 카테고리 관리

사용자는 상품을 분류하기 위해 카테고리를 생성하고 조회할 수 있다.

#### Scenario 1-1: 카테고리 생성

```
Given: 아무런 카테고리가 없는 상태
When:  POST /api/categories (name="교환권")
Then:  응답 200, body에 id(not null)와 name("교환권") 포함
```

#### Scenario 1-2: 카테고리 목록 조회

```
Given: "교환권", "상품권" 두 카테고리가 존재하는 상태
When:  GET /api/categories
Then:  응답 200, 2개의 카테고리가 목록에 포함
```

### Feature 2: 상품 관리

사용자는 카테고리에 속하는 상품을 생성하고 조회할 수 있다.

#### Scenario 2-1: 상품 생성

```
Given: "교환권" 카테고리가 존재하는 상태
When:  POST /api/products (name="스타벅스 아메리카노", price=4500, imageUrl="...", categoryId=카테고리ID)
Then:  응답 200, body에 id, name, price, imageUrl 포함
```

#### Scenario 2-2: 상품 목록 조회

```
Given: 상품이 1개 존재하는 상태
When:  GET /api/products
Then:  응답 200, 1개의 상품이 목록에 포함
```

### Feature 3: 선물하기

사용자는 옵션을 선택하여 다른 회원에게 선물할 수 있다. 선물하면 해당 옵션의 재고가 차감된다.

#### Scenario 3-1: 선물하기 성공

```
Given: 재고 100인 옵션과 회원 2명(보내는 사람, 받는 사람)이 존재하는 상태
When:  POST /api/gifts (optionId, quantity=3, receiverId, message="생일 축하해!")
       Header: Member-Id = 보내는사람ID
Then:  응답 200
       DB에서 해당 옵션의 재고가 97로 감소
```

#### Scenario 3-2: 재고 부족으로 선물 실패

```
Given: 재고 5인 옵션이 존재하는 상태
When:  POST /api/gifts (optionId, quantity=10, ...)
Then:  응답 500 (에러)
       DB에서 해당 옵션의 재고가 5로 유지 (롤백 검증)
```

---

## 테스트 구조 설계

### 왜 이 구조인가

```
src/test/java/gift/
├── model/
│   └── OptionTest.java          ← 유일한 단위 테스트
├── ApiTest.java                 ← 인수 테스트 공통 베이스
├── CategoryApiTest.java         ← Feature 1
├── ProductApiTest.java          ← Feature 2
└── GiftApiTest.java             ← Feature 3
```

**단위 테스트는 `OptionTest` 하나만 존재한다.** `Option.decrease()`는 외부 의존성이 없는 순수 도메인 로직이면서, 조건 분기가 있는 유일한 메서드다. 인수 테스트로도 간접 검증되지만, 이 로직은 빠르게 피드백을 받을 수 있는 단위 테스트의 가치가 분명하다.

**나머지는 모두 인수 테스트다.** `CategoryService.create()`를 단위 테스트하면 `repository.save()`를 호출하는지 verify하게 되는데, 이는 구현에 결합된 테스트다. 대신 인수 테스트로 "API를 호출하면 실제로 생성되고 조회되는가"를 검증하면, 내부 구현이 바뀌어도 테스트는 깨지지 않는다.

### ApiTest 베이스 클래스

모든 인수 테스트가 공유하는 관심사를 분리한다.

```
역할:
- 실제 서버 기동 (@SpringBootTest, RANDOM_PORT)
- RestAssured 포트 설정
- 테스트 간 격리를 위한 DB 정리 (@BeforeEach)

DB 정리 순서 (FK 제약 조건 준수):
  wish → option → product → category → member
```

인수 테스트에서 `@Transactional` 롤백을 쓰지 않는 이유: RestAssured는 실제 HTTP를 보내므로 서버 코드는 **별도 스레드**에서 실행된다. 테스트의 `@Transactional`은 서버의 트랜잭션에 영향을 주지 않는다.

### 시나리오별 테스트 클래스 매핑

| Feature | 클래스 | 시나리오 | 메서드명 |
|---------|--------|---------|---------|
| 카테고리 | `CategoryApiTest` | 1-1. 생성 | `create_validRequest_returnsCreatedCategory` |
| | | 1-2. 목록 조회 | `retrieve_afterCreation_returnsCategoryList` |
| 상품 | `ProductApiTest` | 2-1. 생성 | `create_validRequest_returnsCreatedProduct` |
| | | 2-2. 목록 조회 | `retrieve_afterCreation_returnsProductList` |
| 선물하기 | `GiftApiTest` | 3-1. 성공 + 재고 차감 | `give_validRequest_decreasesStock` |
| | | 3-2. 재고 부족 실패 | `give_insufficientStock_fails` |
| (단위) | `OptionTest` | 정상 차감 | `decrease_sufficientStock_reducesQuantity` |
| | | 부족 시 예외 | `decrease_insufficientStock_throwsException` |

### 테스트 데이터 전략

인수 테스트에서 사전 조건(Given)을 만드는 두 가지 방법이 있다.

| 방법 | 설명 | 사용 시점 |
|------|------|----------|
| **Repository로 직접 시드** | `categoryRepository.save(new Category("교환권"))` | 테스트 대상이 아닌 사전 데이터 (예: Product 테스트에서 Category 준비) |
| **API 호출로 시드** | `POST /api/categories`로 생성 | 테스트 대상 자체인 경우 (예: Category 목록 조회 테스트에서 Category 생성) |

Gift 테스트의 Given은 Category, Product, Option, Member가 모두 필요한데, 이들은 테스트 대상이 아니므로 Repository로 직접 넣는다. API를 통해 넣으면 Gift 테스트가 Category/Product API에 의존하게 되어 테스트 독립성이 깨진다.
