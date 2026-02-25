# Spring Gift 프로젝트

## 프로젝트 정보

- Java 21, Spring Boot 3.5.8, JPA, H2
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`

## 도메인 관계

```
Category 1 --- N Product 1 --- N Option
                Product 1 --- N Wish
Member   1 --- N Wish
```

## API 엔드포인트

| 메서드  | 경로              | 설명      | 헤더        |
|------|-----------------|---------|-----------|
| POST | /api/categories | 카테고리 생성 | -         |
| GET  | /api/categories | 카테고리 목록 | -         |
| POST | /api/products   | 상품 생성   | -         |
| GET  | /api/products   | 상품 목록   | -         |
| POST | /api/gifts      | 선물 전달   | Member-Id |

## 테스트 전략

### 테스트 종류 및 위치

```
src/test/java/gift/
├── acceptance/     # 인수테스트 (API 레벨, RestAssured)
├── service/        # 서비스 단위 테스트
└── model/          # 도메인 단위 테스트
```

### 공통 규칙

- 메서드명은 한글로 시나리오를 설명한다 (예: `상품_등록_성공()`)
- 각 테스트는 독립적으로 실행 가능해야 한다
- given-when-then 패턴을 따른다

### 인수테스트 작성

인수테스트는 `/acceptance-test` skill을 사용한다.
