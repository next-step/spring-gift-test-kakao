# 개발과정
## 1. claude.md 생성

## 2. 프로젝트 분석
### 1) 선물하기 기능 분석

#### 요청

```
POST /api/gifts
Header: Member-Id: 1       ← 보내는 사람
Body: {
  "optionId": 1,            ← 어떤 옵션(상품 변형)을
  "quantity": 2,             ← 몇 개
  "receiverId": 2,           ← 누구에게
  "message": "생일 축하해!"    ← 메시지
}
```

#### 처리 흐름 (6단계)

```
① GiftRestController.give()
   │  @RequestBody로 JSON 파싱, @RequestHeader("Member-Id")로 보내는 사람 ID 추출
   ▼
② GiftService.give()              ← @Transactional (여기서부터 하나의 트랜잭션)
   │
   ├─③ optionRepository.findById(optionId)
   │     → Option 엔티티 조회. 없으면 NoSuchElementException
   │
   ├─④ option.decrease(quantity)
   │     → 재고 >= 요청수량 확인. 부족하면 IllegalStateException
   │     → 통과하면 this.quantity -= quantity (재고 차감)
   │
   ├─⑤ new Gift(memberId, receiverId, option, quantity, message)
   │     → 비영속 객체 생성 (DB 저장 안 됨)
   │
   └─⑥ giftDelivery.deliver(gift)
         → FakeGiftDelivery: 보내는 사람 이름 조회 후 콘솔 출력만 수행
         → (실제 카카오 API 호출은 아직 미구현)

트랜잭션 커밋 → Option의 재고 변경이 DB에 반영됨
```

#### 성공 시

- **응답:** `200 OK` (본문 없음)
- **DB 변경:** Option 테이블의 quantity만 감소
- **콘솔 출력:** `김철수아이폰 16아이폰 16 128GB48` (이름+상품+옵션+남은수량)

#### 실패 케이스

| 상황 | 예외 | 결과 |
|------|------|------|
| 옵션이 존재하지 않음 | `NoSuchElementException` | 500 에러, 아무 변경 없음 |
| 재고 부족 | `IllegalStateException` | 500 에러, 재고 변경 없음 (롤백) |
| 보내는 사람 ID 없음 | `NoSuchElementException` (FakeGiftDelivery에서) | 500 에러, 재고 변경 없음 (롤백) |

#### 주요 특징

- **Gift 객체는 DB에 저장되지 않음** — `@Entity`가 아닌 일반 객체로, 전달 정보를 담는 용도
- **재고 차감은 JPA 더티체킹**으로 반영 — `option.decrease()` 호출만 하고 별도 `save()` 없이 트랜잭션 커밋 시 자동 UPDATE
- **GiftDelivery는 전략 패턴** — 인터페이스로 추상화되어 있고, 현재는 `FakeGiftDelivery`(콘솔 출력)만 존재
- **받는 사람(receiverId) 검증 없음** — 존재하지 않는 회원 ID를 넣어도 에러가 나지 않음 (FakeGiftDelivery가 receiver를 조회하지 않으므로)

### 2) 카테고리 등록 기능 분석

#### 요청

```
POST /api/categories
Body: {
  "name": "식품"       ← 카테고리 이름
}
```

#### 처리 흐름 (3단계)

```
① CategoryRestController.create()
   │  CreateCategoryRequest로 요청 파싱 (@RequestBody 없음 — 폼 바인딩)
   ▼
② CategoryService.create()              ← @Transactional (클래스 레벨)
   │
   └─③ categoryRepository.save(new Category(name))
         → Category 엔티티 생성 후 DB 저장
         → 저장된 Category 객체 반환 (id 자동 생성)

트랜잭션 커밋 → Category가 DB에 반영됨
```

#### 성공 시

- **응답:** `200 OK` + 저장된 Category JSON (`{ "id": 1, "name": "식품" }`)
- **DB 변경:** Category 테이블에 새 행 추가

#### 실패 케이스

| 상황 | 예외 | 결과 |
|------|------|------|
| name이 null | DB 제약 조건 위반 가능 | 500 에러, 카테고리 생성 안 됨 |

#### 주요 특징

- **`@RequestBody` 누락** — 컨트롤러의 `create()` 메서드에 `@RequestBody`가 없어 JSON이 아닌 폼 파라미터(`application/x-www-form-urlencoded`)로 바인딩됨
- **입력 검증 없음** — name에 대한 `@NotNull`, `@NotBlank` 등 Bean Validation이 없음
- **응답이 엔티티 직접 반환** — DTO 없이 `Category` JPA 엔티티를 그대로 JSON 직렬화하여 반환
- **조회 API도 존재** — `GET /api/categories`로 전체 카테고리 목록 조회 가능 (`categoryRepository.findAll()`)

#### 조회 요청

```
GET /api/categories
→ 응답: [{ "id": 1, "name": "식품" }, { "id": 2, "name": "전자기기" }, ...]
```

### 3) 상품 등록 기능 분석

#### 요청

```
POST /api/products
Body: {
  "name": "아이폰 16",         ← 상품 이름
  "price": 1500000,            ← 가격
  "imageUrl": "https://...",   ← 상품 이미지 URL
  "categoryId": 1              ← 소속 카테고리 ID
}
```

#### 처리 흐름 (4단계)

```
① ProductRestController.create()
   │  CreateProductRequest로 요청 파싱 (@RequestBody 없음 — 폼 바인딩)
   ▼
② ProductService.create()              ← @Transactional (클래스 레벨)
   │
   ├─③ categoryRepository.findById(categoryId)
   │     → Category 엔티티 조회. 없으면 NoSuchElementException
   │
   └─④ productRepository.save(new Product(name, price, imageUrl, category))
         → Product 엔티티 생성 후 DB 저장
         → 저장된 Product 객체 반환 (id 자동 생성)

트랜잭션 커밋 → Product가 DB에 반영됨
```

#### 성공 시

- **응답:** `200 OK` + 저장된 Product JSON (`{ "id": 1, "name": "아이폰 16", "price": 1500000, "imageUrl": "https://...", "category": { "id": 1, "name": "전자기기" } }`)
- **DB 변경:** Product 테이블에 새 행 추가 (category_id FK 포함)

#### 실패 케이스

| 상황 | 예외 | 결과 |
|------|------|------|
| categoryId가 존재하지 않음 | `NoSuchElementException` | 500 에러, 상품 생성 안 됨 |
| categoryId가 null | `InvalidDataAccessApiUsageException` | 500 에러, 상품 생성 안 됨 |

#### 주요 특징

- **카테고리 의존** — 상품 등록 시 반드시 유효한 categoryId가 필요. `categoryRepository.findById().orElseThrow()`로 검증
- **`@RequestBody` 누락** — 카테고리와 동일하게 폼 바인딩으로 동작
- **입력 검증 없음** — name, price, imageUrl에 대한 Bean Validation 없음. price가 `int`이므로 값을 안 보내면 기본값 0으로 바인딩됨
- **응답이 엔티티 직접 반환** — Product가 Category를 `@ManyToOne`으로 참조하므로, 응답 JSON에 Category 객체가 중첩되어 포함됨
- **조회 API도 존재** — `GET /api/products`로 전체 상품 목록 조회 가능 (`productRepository.findAll()`)
- **Option 없이 등록** — 상품 등록 시 Option(상품 변형)은 함께 생성되지 않음. Option은 별도로 추가해야 함

#### 조회 요청

```
GET /api/products
→ 응답: [{ "id": 1, "name": "아이폰 16", "price": 1500000, "imageUrl": "https://...", "category": { "id": 1, "name": "전자기기" } }, ...]
```


# 프롬프트 기록

- 이 코드를 분석해서 project.md 파일을 작성해줘 
- 지금 DB 연결이랑 kakao api 설정같은게 있던데 지금 어디까지 구현됐고, 어떻게 작동되는건지 분석해서 알려줘
- 우선 선물주기 기능부터 어떤 흐름으로 작동되는지 분석해서 설명해줘
- ❯ 그럼 이 기능에서 어떤 테스트 시나리오를 만드는게 좋을까?
  예를 들어서, 재고 하나남은 상품을 2번 선물하면 두번쨰는 에러가 나는 테스트 케이스처럼 인수테스트로 작성할만한
  시나리오를 알려줘.

  검증할 행위 목록: 어떤 행위를 선택했는가? 기준은?
  테스트 데이터 전략: 어떻게 준비하고 정리하는가?
  검증 전략: 무엇을 어떻게 검증하는가?
  주요 의사결정: 논의 과정에서 중요한 선택과 이유

  위의 포맷대로 생각해서 몇 가지 알려줘
- 정리된 시나리오를 TEST_STRATEGY.md 파일로 만들어줘

- @docs/PROJECT_HISTORY.md 에 카테고리 등록 기능을 1)선물하기 기능 밑에 2)카테고리 등록 기능으로 분석해서 적어줘
- 방금 작성한 내용을 바탕으로 @docs/TEST_STRATEGY.md 파일에 행위 2로 작성해줘
- 2-4로 중복등록 불가 테스트도 넣어줘

- @docs/PROJECT_HISTORY.md 에 상품 등록 기능을 2)카테고리 등록 기능 밑에 3)상품 등록 기능으로 분석해서 적어줘
- 방금 작성한 내용을 바탕으로 @docs/TEST_STRATEGY.md 파일에 행위 3으로 작성해줘

- 현재 /GET 엔드포인트는 findall로 전체 DB를 조회해오는 건데, 이건 어떤식으로 테스트코드를 작성할 수 있을까?
  단, 기존에 있는 api명세를 바꾸면 안돼.
- 현재 @docs/TEST_STRATEGY.md에 행위 3개가 있는데 여기에 /GET 하는 api 두개도 새로운 행위로 등록하려고 해. 이때
  인수테스트를 작성하려면 기능을 어떻게 나눠야할까?

- 이제 테스트코드를 작성할 건데, 작성하기 전에 @docs/TEST_STRATEGY.md에 @SpringBootTest랑 @RestAssured를 이용해서 인수테스트를 작성하게끔 문서를 수정해줘.
- 테스트 데이터는 어떻게 준비할 것인가?
  초기 상태를 어떻게 설정하는가?
  API로 준비? 다른 방법?
  테스트 간 격리는 어떻게?
  위 내용에 대해서 어떻게 생각하는지 알려줘. 우리가 테스트할때 어떻게 하면 좋을까?
- Mocking이나 Stubbing을 활용할 수 있는 방법은 없어?
- 그럼 아까 말한대로 반영해줘
- 