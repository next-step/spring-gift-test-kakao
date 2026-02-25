# CLAUDE.md

## 프로젝트 요약

카카오 메시지 API를 활용한 선물하기 서비스 (Spring Boot 기반)

## 기술 스택

- Java 21, Gradle 8.4, Spring Boot 3.5.8
- 현재 H2 인메모리 DB, 향후 PostgreSQL + Testcontainers 전환 예정

## 빌드 및 실행

```bash
./gradlew build          # 프로젝트 빌드
./gradlew test           # 전체 테스트 실행
./gradlew bootRun        # 애플리케이션 실행
./gradlew test --tests "gift.SomeTestClass.someMethod"  # 단일 테스트 실행
```

## 아키텍처

`gift/` 패키지 하위 3계층 구조:

- **`ui`** — REST 컨트롤러 (`/api/products`, `/api/categories`, `/api/gifts`)
- **`application`** — 서비스 (@Transactional 비즈니스 로직) 및 요청 DTO
- **`model`** — JPA 엔티티, Spring Data 리포지토리, 도메인 인터페이스 (`GiftDelivery`)
- **`infrastructure`** — 외부 연동 구현체 (`FakeGiftDelivery`, 카카오 API 프로퍼티)

의존성 방향: `ui → application → model` ← `infrastructure`

## 핵심 도메인 모델

- **Product** — Category에 속하며, 여러 Option을 가짐
- **Option** — 상품 변형. 수량(quantity) 관리, `decrease()`로 재고 차감
- **Member** — id, name, email. Wish(위시리스트)를 가짐
- **Gift** — 비영속 객체(JPA 엔티티 아님). Option, 보내는/받는 사람 ID, 수량, 메시지를 담음

## 주요 API

- `POST /api/gifts` — 선물 보내기 (`Member-Id` 헤더로 보내는 사람 식별)
- `GET/POST /api/products` — 상품 목록 조회 / 등록
- `GET/POST /api/categories` — 카테고리 목록 조회 / 등록

## 설정

- `@ConfigurationPropertiesScan`으로 `KakaoMessageProperties`, `KakaoSocialProperties` 로드
- `spring.jpa.open-in-view=false` — 트랜잭션 외부에서 지연 로딩 불가

## 개발 원칙

- 외부 행동(external behavior)은 절대 변경하지 않는다 (API 응답, HTTP 상태 코드, 예외 타입, DB 최종 상태 유지)
- 보호 대상은 **최종 결과**이지 메서드 호출이 아니다
- 상세 규칙(리팩터링 금지/허용 사항, 비즈니스 요구사항, 테스트 전략)은 `/bdd` 스킬 참조
