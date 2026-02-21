# Project

Spring Boot 기반 선물/상품 관리 시스템. Java 21, Gradle, H2 인메모리 DB 사용.

# Build & Test

- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 단일 테스트: `./gradlew test --tests "gift.acceptance.GiftAcceptanceTest.giftDecreasesOptionQuantity"`

# Code Style

- 패키지 구조: `gift.model` (엔티티/리포지토리), `gift.application` (서비스/DTO), `gift.ui` (컨트롤러), `gift.infrastructure` (외부 연동)
- 엔티티는 `protected` 기본 생성자 + public 생성자 패턴
- DTO는 private 필드 + getter만 (setter 없음)

# Testing

- 인수 테스트만 작성 (단위 테스트/Mock/Spy 사용 금지)
- `@SpringBootTest(RANDOM_PORT)` + RestAssured로 HTTP 경계에서 검증
- `@Sql`로 테스트 데이터 관리, SQL 파일은 `src/test/resources/sql/`에 위치
- 테스트 격리: cleanup.sql(TRUNCATE) → data.sql 순서로 매 테스트 전 실행
- 검증은 HTTP 응답 + DB 상태를 조합 (최소 2가지 이상)
- `@DisplayName`에 검증할 행위를 한글로 서술

# Git

- AngularJS 커밋 스타일: `type(scope): subject`
- 작업 단위를 논리적으로 분리하여 커밋
- 각 커밋 상태에서 테스트가 통과해야 함
