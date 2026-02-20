# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 핵심 원칙

- **main 코드 수정 금지**: `src/main/` 하위의 코드를 수정하거나 추가하지 않는다. 오직 `src/test/` 하위의 테스트 코드만 작성한다.
- `build.gradle`의 `testImplementation` 의존성 추가는 테스트 인프라이므로 허용한다.

## Build & Run Commands

```bash
./gradlew build          # Build and run tests
./gradlew bootRun        # Run the application
./gradlew test           # Run all tests
./gradlew test --tests "gift.SomeTest"              # Run a single test class
./gradlew test --tests "gift.SomeTest.methodName"   # Run a single test method
```

Java 21 is required. The project uses Gradle 8.4 with the wrapper (`./gradlew`).

## Architecture

Spring Boot 3.5.8 gift-giving platform. H2 in-memory DB + Spring Data JPA.

```
ui (REST Controllers)  →  application (Services + DTOs)  →  model (Entities + Repositories)
                                                              ↑
                                                     infrastructure (Config + Implementations)
```

### 코드 작성 시 알아야 할 제약사항

- **`spring.jpa.open-in-view=false`**: Lazy loading은 `@Transactional` 범위 안에서만 동작한다.
- **DTO binding 제약**: Category/Product 컨트롤러는 `@ModelAttribute` 바인딩을 사용하지만 DTO에 setter가 없어 form param이 바인딩되지 않는다. 테스트에서는 Repository로 직접 시드한다.
- **전역 에러 핸들링 없음**: `@ControllerAdvice` 부재로 모든 예외가 raw 500 응답.

## 테스트 코드 컨벤션

| 항목 | 규칙 | 예시 |
|------|------|------|
| 메서드명 | `methodUnderTest_state_expectedBehavior` | `decrease_insufficientStock_throwsException` |
| `@DisplayName` | 한국어로 행위를 서술 | `"재고보다 많은 수량을 차감하면 예외가 발생한다"` |
| 구조 | Arrange-Act-Assert 주석으로 구분 | `// Arrange`, `// Act`, `// Assert` |
| 인수 테스트 검증 | HTTP 응답만 검증 (상태 코드, body) | DB 상태 검증은 단위 테스트 영역 |
| 테스트 데이터 | Repository로 직접 시드 | `categoryRepository.save(new Category("교환권"))` |
| 독립성 | 테스트 간 상태 공유 없음 | `@BeforeEach`에서 전체 정리 |

```bash
./gradlew test   # 4개 클래스, 8개 메서드 모두 통과해야 한다
```

## 참조 목록

```
docs/
├── SYSTEM_OVERVIEW.md  # 도메인 모델, 패키지 구조, API 엔드포인트, 핵심 플로우
├── TEST_DESIGN.md      # 단위 vs 인수 테스트 개념, 시나리오 정의, 테스트 구조
├── TEST_STRATEGY.md    # 행위 선정 기준, 데이터 전략, 검증 전략, 의사결정 근거
```
