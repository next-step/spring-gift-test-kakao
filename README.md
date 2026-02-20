# Spring Gift Service

Spring Boot 기반 선물하기 서비스 애플리케이션입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.8
- Spring Data JPA
- H2 Database
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
