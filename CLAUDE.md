# 선물하기 API 테스트 프로젝트

## 프로젝트 개요
Spring Boot 3.5.8 기반 선물하기(Gift) API. 카테고리/상품/옵션 관리 및 회원 간 선물 전송 기능을 제공한다.

## 기술 스택
- Java 17, Spring Boot 3.5.8, Spring Data JPA, H2 (in-memory DB)
- 테스트: JUnit 5, RestAssured (`spring-boot-starter-test`, `io.rest-assured:rest-assured`)

## 프로젝트 구조
```
src/main/java/gift/
├── ui/                  # REST 컨트롤러
│   ├── CategoryRestController.java    # POST/GET /api/categories
│   ├── ProductRestController.java     # POST/GET /api/products
│   └── GiftRestController.java        # POST /api/gifts
├── application/         # 서비스 + Request DTO
│   ├── CategoryService.java
│   ├── ProductService.java
│   ├── OptionService.java
│   ├── WishService.java
│   ├── GiftService.java
│   └── *Request.java (CreateCategoryRequest, CreateProductRequest, GiveGiftRequest 등)
├── model/               # JPA 엔티티 + 리포지토리 + 인터페이스
│   ├── Category.java, Product.java, Option.java, Member.java, Wish.java
│   ├── Gift.java              # 값 객체 (JPA 엔티티 아님)
│   ├── GiftDelivery.java      # 선물 배달 인터페이스
│   └── *Repository.java
└── infrastructure/      # 외부 연동 구현체
    ├── FakeGiftDelivery.java  # GiftDelivery 구현 (콘솔 출력)
    └── Kakao*Properties.java
```

## 빌드 및 테스트 명령어
```bash
./gradlew build          # 전체 빌드
./gradlew test           # 테스트 실행
./gradlew test --tests "gift.model.OptionTest"  # 특정 테스트 클래스 실행
```

## 테스트 작성 시 주의사항

### 인수테스트 방식
- `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` + RestAssured로 실제 HTTP 요청을 보내는 인수테스트를 작성한다.
- `@LocalServerPort`로 할당된 포트를 받아 `RestAssured.port`에 설정한다.

### 컨트롤러 바인딩 방식
- `CategoryRestController.create()`, `ProductRestController.create()`: **`@RequestBody` 없음** → 폼 파라미터로 바인딩됨. RestAssured에서 `.formParam("name", "값")` 사용.
- `GiftRestController.give()`: **`@RequestBody` 있음** + `@RequestHeader("Member-Id")` → `.contentType(JSON).header("Member-Id", id).body(map)` 사용.

### GiftDelivery 의존성
- `GiftService`는 `GiftDelivery` 인터페이스에 의존. 테스트 시 `@MockitoBean`으로 대체하여 외부 의존성(카카오 API 등)을 격리한다.

### 테스트 데이터 관리
- RestAssured는 별도 스레드에서 실제 HTTP 요청을 보내므로 **`@Transactional` 롤백이 동작하지 않는다.**
- `@BeforeEach`에서 `RestAssured.port` 설정 및 필요한 사전 데이터를 생성한다.
- `@AfterEach`에서 Repository의 `deleteAllInBatch()`를 호출하여 테스트 데이터를 정리한다. 외래키 의존 순서에 주의하여 자식 엔티티부터 삭제한다.

## 사용자 행위 목록 (테스트 대상)
1. 카테고리를 생성한다
2. 카테고리 목록을 조회한다
3. 상품을 등록한다 (카테고리 필요)
4. 상품 목록을 조회한다
5. 선물을 보낸다 (옵션 재고 차감 + 배달)

## 코드 컨벤션
- 테스트 메서드명: `행위_조건_기대결과` 패턴 (예: `decrease_insufficientStock_throwsException`)
- `@DisplayName`으로 한글 설명 추가
- RestAssured BDD 스타일 사용: `given().when().then()`

## build.gradle 테스트 의존성
```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'io.rest-assured:rest-assured'
```
