# 프로젝트 개요

선물하기(Gift) 서비스 — Spring Boot 3.5.8, Java 25, H2 인메모리 DB

## 프로젝트 구조

```
gift.ui              → REST 컨트롤러
gift.application     → 서비스 + 요청 DTO
gift.model           → JPA 엔티티, Repository, 포트 인터페이스
gift.infrastructure  → GiftDelivery 구현체 (FakeGiftDelivery)
```

## API 엔드포인트

| 메서드 | 경로 | 바인딩 방식 |
|--------|------|------------|
| POST | /api/categories | Query/Form Parameter |
| GET | /api/categories | - |
| POST | /api/products | Query/Form Parameter (categoryId 필요) |
| GET | /api/products | - |
| POST | /api/gifts | JSON Body (`@RequestBody`) + `Member-Id` 헤더 |

## 테스트 규칙

- **인수 테스트(Acceptance Test)**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `RestAssured`
- **데이터 준비**: `@Sql` 어노테이션으로 SQL 스크립트 실행. API/Service/Repository 직접 호출 금지
- **테스트 격리**: 매 테스트 전 `cleanup.sql` 실행. `@Transactional`, `@DirtiesContext` 사용 금지
- **검증 방식**: "호출 여부"가 아닌 "결과 상태"를 검증. API 응답과 후속 API 호출로 확인
- **메서드 네이밍**: 한글 메서드명 사용 (예: `선물을_보내면_재고가_차감된다`)
- **클래스 네이밍**: `{도메인}AcceptanceTest` (예: `GiftAcceptanceTest`)
- **베이스 클래스**: `AcceptanceTest`를 상속

## SQL 스크립트 위치

```
src/test/resources/sql/
├── cleanup.sql        # DELETE + IDENTITY 리셋 (외래키 역순)
├── category-data.sql  # 카테고리만
├── product-data.sql   # 상품만 (category-data.sql에 의존)
├── option-data.sql    # 옵션만 (product-data.sql에 의존)
└── member-data.sql    # 회원만
```

## 주의사항

- Category, Product 생성은 Query Parameter. Gift 생성만 JSON Body — 테스트 시 바인딩 방식 혼동 주의
- Option, Member는 REST API 없음 — `@Sql`로 데이터 준비
- 에러 핸들링 미구현 — 실패 시 5xx 응답
