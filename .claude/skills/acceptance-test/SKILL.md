---
name: acceptance-test
description: 인수테스트를 작성한다. 사용자가 인수테스트, 수락테스트, API 테스트 작성을 요청할 때 사용.
argument-hint: "[테스트할 시나리오]"
---

$ARGUMENTS 에 대한 인수테스트를 작성한다.

## 작성 절차

1. 먼저 테스트 대상 API의 컨트롤러, 서비스, 도메인 코드를 Read로 확인한다.
2. 기존 인수테스트 파일이 있으면 읽어서 패턴을 따른다.
3. CLAUDE.md의 "인수테스트 작성 규칙"을 반드시 준수한다.
4. 테스트를 작성한 후 `./gradlew test --tests "테스트클래스명"`으로 실행하여 통과를 확인한다.

## 테스트 구조

- 위치: `src/test/java/gift/acceptance/`
- 클래스명: `*AcceptanceTest`
- 메서드명: 한글 시나리오 설명 (예: `선물하기_성공시_재고가_요청_수량만큼_감소한다()`)
- 패턴: Given-When-Then (`// given`, `// when`, `// then`)

## 기본 클래스 구조

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XxxAcceptanceTest {

    @LocalServerPort
    int port;

    // 공개 API가 없는 엔티티는 Repository를 @Autowired로 주입
    @Autowired
    MemberRepository memberRepository;

    @Autowired
    OptionRepository optionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        // DB 초기화: FK 순서(자식→부모)로 deleteAllInBatch()
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();

        // 공통 테스트 데이터 생성
    }
}
```

## 데이터 생성 규칙

| 데이터      | 방법                            | 이유        |
|----------|-------------------------------|-----------|
| Category | `POST /api/categories` API 호출 | 공개 API 존재 |
| Product  | `POST /api/products` API 호출   | 공개 API 존재 |
| Option   | `OptionRepository.save()`     | 공개 API 없음 |
| Member   | `MemberRepository.save()`     | 공개 API 없음 |

- ID 하드코딩 금지. 생성 결과에서 동적으로 참조한다.
- `FakeGiftDelivery`가 sender를 DB에서 조회하므로 sender Member는 반드시 사전 생성한다.

## 헬퍼 메서드

반복되는 API 호출은 테스트 클래스 내 private 메서드로 추출한다:

```java
private Option 옵션_생성(int quantity) {
    var category = categoryRepository.save(new Category("음료"));
    var product = productRepository.save(new Product("아메리카노", 4500, "http://example.com/image.jpg", category));
    return optionRepository.save(new Option("ICE", quantity, product));
}

private ExtractableResponse<Response> 선물_전달(Long optionId, int quantity) {
    return RestAssured.given().log().all()
            .contentType(ContentType.JSON)
            .header("Member-Id", senderId)
            .body(Map.of(
                    "optionId", optionId,
                    "quantity", quantity,
                    "receiverId", receiverId,
                    "message", "선물입니다"
            ))
            .when()
            .post("/api/gifts")
            .then().log().all()
            .extract();
}
```

## 검증 규칙

1. **HTTP 응답**: 상태코드를 반드시 검증한다.
2. **DB 상태**: 핵심 도메인 규칙은 Repository 재조회로 DB 최종 상태까지 검증한다.
3. **실패 후 불변**: 실패 시나리오에서는 상태코드 + 데이터 불변을 함께 검증한다.

```java
// 실패 검증 예시
var before = optionRepository.findById(optionId).get().getQuantity();
var response = 선물_전달(optionId, before + 1);
assertThat(response.statusCode()).isEqualTo(500);
assertThat(optionRepository.findById(optionId).get().getQuantity()).isEqualTo(before);
```

## 주의사항

- CategoryRestController, ProductRestController의 create에는 `@RequestBody`가 없다. form param으로 바인딩된다.
- GiftRestController의 give에는 `@RequestBody`가 있다. JSON body로 전송한다.
- 현재 예외 핸들러(`@ExceptionHandler`)가 없으므로 예외 시 500이 반환된다. 핸들러 추가 후 적절한 상태코드(400, 404)로 수정한다.
