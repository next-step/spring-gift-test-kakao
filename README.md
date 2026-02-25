# spring-gift-test

카카오 메시지 API를 활용한 선물하기 서비스 (Spring Boot 기반)

## 기술 스택

- Java 21, Spring Boot 3.5.8, Gradle 8.4
- Spring Data JPA + H2 인메모리 DB
- RestAssured (인수 테스트)

## 주요 기능

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/gifts` | POST | 선물 보내기 (재고 차감 + 카카오 메시지 전송) |
| `/api/products` | GET/POST | 상품 목록 조회 / 등록 |
| `/api/categories` | GET/POST | 카테고리 목록 조회 / 등록 |

## 실행

```bash
./gradlew bootRun          # 애플리케이션 실행
./gradlew test             # JUnit 인수 테스트 (H2, Docker 불필요)
./gradlew cucumberTest     # Cucumber 시나리오 테스트 (Docker로 App + PostgreSQL 자동 실행)
```

## 프로젝트 구조

```
gift/
├── ui/              # REST 컨트롤러
├── application/     # 서비스 + 요청 DTO
├── model/           # JPA 엔티티 + Repository
└── infrastructure/  # 외부 연동 (FakeGiftDelivery, 카카오 API 설정)
```

## 과제 진행 과정

1. CLAUDE.md 작성 — 프로젝트 요구사항 및 개발 제약사항 정리
2. 기능 분석 — 선물하기, 카테고리/상품 등록, 카테고리/상품 조회 흐름 파악
3. 테스트 전략 수립 — 인수 테스트 시나리오 및 데이터 준비 전략 설계
4. 테스트 코드 작성 — `@SpringBootTest` + RestAssured 기반 인수 테스트 구현

상세 개발 과정은 [docs/PROJECT_HISTORY.md](docs/PROJECT_HISTORY.md) 참고
