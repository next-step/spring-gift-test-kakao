# Spring Gift Service

Spring Boot 기반 선물하기 서비스 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.8
- Spring Data JPA
- H2 Database (기본)
- PostgreSQL 16 (Docker Compose)
- Gradle

## 주요 기능

- **상품 관리**: 상품 등록 및 조회
- **카테고리 관리**: 카테고리 등록 및 조회
- **옵션 관리**: 상품별 옵션(색상, 사이즈 등) 관리 및 재고 추적
- **위시리스트**: 회원별 관심 상품 등록
- **선물하기**: 다른 회원에게 선물 전송 (카카오 메시지 연동)

## API 엔드포인트

| Method | Path             | 설명              |
|--------|------------------|-------------------|
| POST   | /api/products    | 상품 등록         |
| GET    | /api/products    | 상품 목록 조회    |
| POST   | /api/categories  | 카테고리 등록     |
| GET    | /api/categories  | 카테고리 목록 조회|
| POST   | /api/gifts       | 선물 전송         |

## 도메인 모델 관계

```
Category (1) ──── (N) Product (1) ──── (N) Option
                         │
                         │ (N)
                         │
Member (1) ──── (N) Wish ┘
   │
   │ (N)
   │
  Gift ──── Option
```

### 엔티티 설명

| 엔티티   | 설명                                           |
|----------|------------------------------------------------|
| Category | 상품 카테고리 (예: 전자기기, 식품)             |
| Product  | 상품 정보 (이름, 가격, 이미지)                 |
| Option   | 상품 옵션 및 재고 (색상, 사이즈 등)            |
| Member   | 회원 정보                                      |
| Wish     | 회원의 위시리스트                              |
| Gift     | 선물 전송 정보 (발신자, 수신자, 옵션, 수량, 메시지) |

## 프로젝트 구조

```
src/main/java/gift/
├── Application.java        # 애플리케이션 진입점
├── ui/                     # REST 컨트롤러
│   ├── ProductRestController.java
│   ├── CategoryRestController.java
│   └── GiftRestController.java
├── application/            # 서비스 및 DTO
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── OptionService.java
│   ├── WishService.java
│   ├── GiftService.java
│   └── *Request.java       # 요청 DTO들
├── model/                  # 엔티티 및 레포지토리
│   ├── Product.java
│   ├── Category.java
│   ├── Option.java
│   ├── Member.java
│   ├── Wish.java
│   ├── Gift.java
│   └── *Repository.java    # JPA 레포지토리들
└── infrastructure/         # 설정 및 외부 연동
    ├── KakaoMessageProperties.java
    ├── KakaoSocialProperties.java
    └── FakeGiftDelivery.java
```

## 실행 방법

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

애플리케이션은 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 테스트 실행 방법

### 전체 테스트

```bash
./gradlew test
```

### Cucumber BDD 테스트

```bash
# Cucumber 전체 실행 (H2, 기본)
./gradlew cucumberTest

# PostgreSQL로 실행 (Docker 자동 시작/종료)
./gradlew cucumberTest -Pdb=postgres

# 태그로 도메인별 실행
./gradlew cucumberTest -Ptag=@category   # 카테고리 (4개 시나리오)
./gradlew cucumberTest -Ptag=@product    # 상품 (4개 시나리오)
./gradlew cucumberTest -Ptag=@gift       # 선물 (5개 시나리오)

# PostgreSQL + 태그 조합
./gradlew cucumberTest -Pdb=postgres -Ptag=@category

# 특정 feature 파일 실행
./gradlew cucumberTest -Pfeature=카테고리_관리
./gradlew cucumberTest -Pfeature=상품_관리
./gradlew cucumberTest -Pfeature=선물_전송
```

### 테스트 실행 모드

| 명령어 | App | DB | RestAssured 대상 |
|--------|-----|----|-----------------|
| `./gradlew cucumberTest` | Embedded | H2 | localhost:{random} |
| `./gradlew cucumberTest -Pdb=postgres` | Embedded | Docker PG | localhost:{random} |
| `./gradlew cucumberTest -Pmode=docker` | Docker Container | Docker PG | localhost:28080 |

- 기본값은 H2이며, `-Pdb=postgres` 옵션 시 Docker Compose로 PostgreSQL 컨테이너를 자동 관리합니다.
- `-Pmode=docker` 옵션 시 앱 자체도 Docker 컨테이너로 실행하여 프로덕션과 동일한 환경에서 테스트합니다.
  - Docker 이미지 빌드 → 컨테이너(app + postgres) 시작 → 테스트 실행 → 컨테이너 종료까지 자동으로 진행됩니다.
  - 앱 컨테이너는 포트 `28080`, PostgreSQL은 포트 `15432`를 사용합니다.
- 수동 Docker 제어:
  - PostgreSQL만: `./gradlew dockerPostgresUp` / `./gradlew dockerPostgresDown`
  - 앱 + PostgreSQL: `./gradlew dockerUp` / `./gradlew dockerDown`

