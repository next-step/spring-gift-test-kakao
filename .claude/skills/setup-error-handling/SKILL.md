---
name: setup-error-handling
description: Spring Boot 프로젝트에 도메인별 에러 처리 구조를 세팅한다
argument-hint: 도메인 이름들 (예: "Order, Payment")
---

$ARGUMENTS 도메인에 대한 에러 처리 구조를 세팅한다. 아래 단계를 순서대로 수행한다.

## 구조 개요

```
error/
├── ErrorCode.java              ← 인터페이스 (code, message, httpStatus)
├── ErrorResponse.java          ← 응답 DTO + 팩토리 메서드
├── BusinessException.java      ← 추상 베이스 예외
├── GlobalExceptionHandler.java ← @RestControllerAdvice
├── CommonErrorCode.java        ← 공통 에러 코드 enum
├── CommonException.java        ← 공통 예외
├── {Domain}ErrorCode.java      ← 도메인별 에러 코드 enum
└── {Domain}Exception.java      ← 도메인별 예외
```

## 1단계: 프로젝트 분석

- 기존 에러 처리 구조가 있는지 확인한다 (기존 구조가 있으면 충돌하지 않게 조정한다)
- base package 경로를 파악한다 (예: `com.example.app`)
- 에러 패키지 위치를 결정한다 (예: `com.example.app.error`)
- $ARGUMENTS에서 도메인 이름들을 추출한다

## 2단계: 공통 에러 인프라 작성

### ErrorCode 인터페이스

모든 에러 코드 enum이 구현할 계약을 정의한다.

```java
public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
```

### ErrorResponse DTO

API 에러 응답의 JSON 구조를 정의한다. 팩토리 메서드 `from(ErrorCode)`를 포함한다.

```java
public class ErrorResponse {
    private final String code;
    private final String message;

    public ErrorResponse(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorResponse from(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    // getter만 제공 (setter 없음)
}
```

### BusinessException 추상 클래스

모든 도메인 예외의 부모. ErrorCode를 감싸고 메시지를 RuntimeException에 전달한다.

```java
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

### GlobalExceptionHandler

`BusinessException`을 잡아서 ErrorCode의 httpStatus와 ErrorResponse로 변환한다.
Spring 프레임워크 예외(MissingRequestHeaderException, MethodArgumentTypeMismatchException, HttpMessageNotReadableException)도 함께 처리한다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
        final ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode));
    }

    // Spring 프레임워크 예외 핸들러들 (INVALID_REQUEST로 매핑)
}
```

## 3단계: CommonErrorCode + CommonException 작성

모든 프로젝트에 공통으로 필요한 에러 코드를 정의한다.

```java
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("INVALID_REQUEST", "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

    // 필드, 생성자, getter
}
```

```java
public class CommonException extends BusinessException {
    public CommonException(final CommonErrorCode errorCode) {
        super(errorCode);
    }
}
```

## 4단계: 도메인별 ErrorCode + Exception 작성

$ARGUMENTS의 각 도메인에 대해 ErrorCode enum과 Exception 클래스를 생성한다.

### 규칙

- **ErrorCode enum**: `{Domain}ErrorCode implements ErrorCode` — 해당 도메인의 에러 코드를 나열한다
- **Exception 클래스**: `{Domain}Exception extends BusinessException` — 생성자가 해당 도메인의 ErrorCode만 받도록 타입을 제한한다
- 에러 코드는 `SCREAMING_SNAKE_CASE`로 작성한다
- 각 도메인에 최소 하나의 `_NOT_FOUND` 에러 코드를 포함한다

### 예시: Order 도메인

```java
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    // 필드, 생성자, getter
}
```

```java
public class OrderException extends BusinessException {
    public OrderException(final OrderErrorCode errorCode) {
        super(errorCode);
    }
}
```

## 5단계: 사용법 안내

작성 완료 후 사용 예시를 안내한다.

### 예외 던지기

```java
throw new OrderException(OrderErrorCode.ORDER_NOT_FOUND);
```

### 응답 형태

```json
{
    "code": "ORDER_NOT_FOUND",
    "message": "주문을 찾을 수 없습니다."
}
```

### 새 도메인 에러 추가 시

1. `{Domain}ErrorCode` enum에 새 상수를 추가한다
2. 서비스에서 `new {Domain}Exception({Domain}ErrorCode.NEW_ERROR)`를 던진다
3. GlobalExceptionHandler가 자동으로 처리한다 (추가 코드 불필요)

## 6단계: 검증

- 컴파일이 되는지 확인한다 (`./gradlew compileJava`)
- 기존 테스트가 깨지지 않는지 확인한다 (`./gradlew test`)
