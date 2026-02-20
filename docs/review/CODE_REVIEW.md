# Code Review Report

## 총평

전반적으로 깔끔한 레이어 구조(Ports & Adapters)를 갖추고 있으며, 테스트 전략(`@Sql` 기반 데이터 준비, `cleanup.sql` 격리)이 잘 설계되어 있습니다. 그러나 **동시성 제어 부재**, **입력 검증 누락**, **엔티티 직접 노출** 등 프로덕션 환경에서 치명적인 문제가 여러 곳에 존재합니다.

---

## 발견된 문제점

### 🔴 치명적 #1 — 재고 차감 시 동시성 제어 부재 (Race Condition)

**파일:** `GiftService.java:24-26`, `Option.java:34-39`

```java
// GiftService.java
final Option option = optionRepository.findById(request.getOptionId()).orElseThrow();
option.decrease(request.getQuantity());
```

`findById()`는 일반 SELECT이므로 행 잠금이 걸리지 않습니다. 두 요청이 동시에 `quantity=10`을 읽은 후 각각 7개씩 차감하면, 둘 다 `10 >= 7` 검증을 통과하여 **재고가 -4개**가 되는 초과 판매(Overselling)가 발생합니다.

**영향:** 선물하기 서비스에서 재고 무결성 파괴 — 실제 재고보다 많은 선물이 전송됨

**개선 방안:**

```java
// OptionRepository.java — 비관적 잠금 추가
public interface OptionRepository extends JpaRepository<Option, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Option o WHERE o.id = :id")
    Optional<Option> findByIdForUpdate(@Param("id") Long id);
}
```

또는 낙관적 잠금(`@Version`)을 사용하여 충돌 시 재시도 로직을 구현할 수 있습니다.

---

### 🔴 치명적 #2 — `Option.decrease()`에 음수/0 수량 가드 누락

**파일:** `Option.java:34-39`

```java
public void decrease(final int quantity) {
    if (this.quantity < quantity) {
        throw new IllegalStateException();
    }
    this.quantity -= quantity;
}
```

`quantity = -5`가 들어오면 조건 `this.quantity < -5`는 항상 거짓이므로 검증을 통과하고, `this.quantity -= (-5)` → **재고가 증가**합니다. 외부에서 수량을 조작하여 무한 재고를 만들 수 있는 보안 취약점입니다.

**개선된 코드:**

```java
public void decrease(final int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
    }
    if (this.quantity < quantity) {
        throw new IllegalStateException("재고가 부족합니다. 현재: " + this.quantity + ", 요청: " + quantity);
    }
    this.quantity -= quantity;
}
```

---

### 🔴 치명적 #3 — 모든 Request DTO에 입력 검증 없음

**파일:** `CreateCategoryRequest.java`, `CreateProductRequest.java`, `GiveGiftRequest.java`

`name`이 `null`이거나 빈 문자열, `price`가 음수, `categoryId`가 `null`인 경우 아무런 검증 없이 서비스 레이어까지 전파됩니다.

특히 `CreateProductRequest.getCategoryId()`가 `null`이면 `categoryRepository.findById(null)`에서 `IllegalArgumentException`이 발생하여, `NoSuchElementException`과 다른 예외 경로를 타게 됩니다.

**개선 방안:** Jakarta Bean Validation 적용

```java
public class CreateProductRequest {
    @NotBlank
    private String name;

    @Positive
    private int price;

    private String imageUrl;

    @NotNull
    private Long categoryId;
    // ...
}
```

컨트롤러에 `@Valid` 추가:

```java
@PostMapping
public Product create(@Valid final CreateProductRequest request) { ... }
```

---

### 🟡 경고 #1 — JPA 엔티티 직접 API 응답으로 노출

**파일:** `CategoryRestController.java:14`, `ProductRestController.java:13`

```java
@PostMapping
public Category create(final CreateCategoryRequest request) {
    return categoryService.create(request);
}
```

엔티티를 직접 반환하면:
- 내부 DB 구조(필드명, 관계)가 API 소비자에게 노출됨
- `@ManyToOne` 관계의 직렬화 시 **순환 참조** 또는 **N+1 문제** 발생 가능
- API 스펙과 도메인 모델이 강결합되어, 엔티티 변경이 곧 API 변경이 됨

**개선 방안:** Response DTO를 분리하여 사용

---

### 🟡 경고 #2 — `@Transactional`이 읽기 전용 메서드에도 적용됨

**파일:** `CategoryService.java`, `ProductService.java`, `OptionService.java`

```java
@Transactional  // 클래스 레벨 — retrieve()에도 쓰기 트랜잭션 적용
@Service
public class CategoryService { ... }
```

`retrieve()` 같은 읽기 전용 메서드에 불필요하게 쓰기 트랜잭션이 걸립니다. DB 커넥션을 불필요하게 오래 점유하고, 일부 DB에서는 불필요한 잠금을 유발합니다.

**개선 방안:**

```java
@Transactional(readOnly = true)  // 클래스 레벨 기본값
@Service
public class CategoryService {
    @Transactional  // 쓰기 메서드만 오버라이드
    public Category create(...) { ... }

    // retrieve()는 클래스 레벨의 readOnly = true 적용
    public List<Category> retrieve() { ... }
}
```

---

### 🟡 경고 #3 — 에러 핸들링 전략 부재

**파일:** 전체 서비스 레이어

```java
optionRepository.findById(request.getOptionId()).orElseThrow();  // NoSuchElementException (메시지 없음)
throw new IllegalStateException();  // 메시지 없음
```

모든 예외가 500 Internal Server Error로 반환되며, 클라이언트는 **무엇이 잘못되었는지 알 수 없습니다.** "없는 옵션"인지 "재고 부족"인지 구분이 불가능합니다.

**개선 방안:** 도메인 예외 + `@RestControllerAdvice`

```java
// 도메인 예외
public class EntityNotFoundException extends RuntimeException { ... }
public class InsufficientStockException extends RuntimeException { ... }

// 글로벌 예외 핸들러
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
}
```

---

### 🟡 경고 #4 — `FakeGiftDelivery`에서 `System.out.println` 사용

**파일:** `FakeGiftDelivery.java:24`

```java
System.out.println(member.getName() + product.getName() + option.getName() + option.getQuantity());
```

프로덕션 환경에서 `System.out`은 로그 수집 도구에 포착되지 않으며, 로그 레벨 제어도 불가합니다. 또한 문자열이 구분자 없이 연결되어 가독성이 없습니다.

**개선된 코드:**

```java
private static final Logger log = LoggerFactory.getLogger(FakeGiftDelivery.class);

@Override
public void deliver(final Gift gift) {
    final Member member = memberRepository.findById(gift.getFrom()).orElseThrow();
    final Option option = gift.getOption();
    final Product product = option.getProduct();
    log.info("[Gift] {} → {} | {} - {} (잔여: {})",
        member.getName(), gift.getTo(), product.getName(), option.getName(), option.getQuantity());
}
```

---

### 🟡 경고 #5 — Request DTO 패턴 불일치

| DTO | Setter | 바인딩 방식 |
|-----|--------|------------|
| `CreateCategoryRequest` | O (setter 있음) | Query Parameter |
| `CreateProductRequest` | O (setter 있음) | Query Parameter |
| `GiveGiftRequest` | X (getter만) | `@RequestBody` JSON |
| `CreateOptionRequest` | X (getter만) | 사용처 없음 (API 미노출) |

Query Parameter 바인딩은 Spring의 `DataBinder`가 setter를 사용하고, `@RequestBody` JSON은 Jackson이 리플렉션/필드 접근을 사용합니다. 동작은 하지만 **일관성이 떨어지며**, 바인딩 방식을 변경할 때 버그가 발생할 수 있습니다.

---

### 🟢 개선 권장 #1 — `KakaoMessageProperties`, `KakaoSocialProperties` 미사용 코드

**파일:** `KakaoMessageProperties.java`, `KakaoSocialProperties.java`

어디에서도 주입되거나 참조되지 않는 데드 코드입니다. `application.properties`에 설정값은 있으나 사용하는 곳이 없습니다.

---

### 🟢 개선 권장 #2 — JPA 엔티티에 `equals()`/`hashCode()` 미구현

**파일:** `Category.java`, `Product.java`, `Option.java`, `Member.java`, `Wish.java`

JPA 엔티티가 `Set`이나 `Map`에 담기거나 detached 상태에서 비교될 때 예기치 않은 동작이 발생할 수 있습니다. ID 기반의 `equals()`/`hashCode()`를 구현하는 것이 안전합니다.

---

### 🟢 개선 권장 #3 — `Product.price`가 `int` 타입

**파일:** `Product.java:15`

정수형 가격은 소수점 금액(4,500.5원) 처리가 불가하고, 큰 금액에서 오버플로우 위험이 있습니다. `BigDecimal` 또는 원 단위 `long` 사용을 권장합니다.

---

## 추가 조언

1. **동시성 테스트를 추가하세요.** `ExecutorService`로 동시 요청을 보내 재고 초과 판매가 발생하지 않는지 검증하는 테스트가 필수적입니다.

2. **도메인 불변식(Invariant)은 엔티티 내부에서 보호하세요.** `Option.decrease()`에 음수 가드를 추가한 것처럼, 엔티티 생성자에서도 `name != null`, `price > 0` 등의 불변식을 검증하면 어느 경로로 생성되든 안전합니다.

3. **API 바인딩 방식을 통일하는 것을 고려하세요.** Category/Product 생성도 JSON Body로 통일하면 테스트 코드도 일관되고, 추후 복잡한 요청 구조 확장이 용이합니다.

4. **테스트 코드는 잘 작성되어 있습니다.** `@Sql` 기반 데이터 준비, `cleanup.sql` 격리, 한글 메서드명, 결과 상태 검증 등 CLAUDE.md의 테스트 규칙을 충실히 따르고 있습니다. `CategoryAcceptanceTest`에서 API로 데이터를 생성하는 부분은 "생성 기능 자체를 테스트"하는 것이므로 적절합니다.
