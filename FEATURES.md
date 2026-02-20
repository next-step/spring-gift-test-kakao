# 핵심 기능 명세서

## 프롬프트

> 이 프로젝트에서 구현하고 있는 핵심 기능을 정리하여 문서로 생성해줘. 각 기능은 사용자가 앱을 통해 실행할 수 있는 행동 단위로 구성되어야 해. 문서는 md 파일로 작성하고 파일에 이 프롬프트 내용을 포함해줘.

---

## 프로젝트 개요

선물하기 플랫폼 API 서버로, 사용자가 카테고리와 상품을 관리하고, 위시리스트를 구성하며, 다른 회원에게 선물을 보낼 수 있는 기능을 제공한다.

- **프레임워크:** Spring Boot 3.5.8
- **언어:** Java 21
- **데이터베이스:** H2 (내장형)
- **데이터 접근:** Spring Data JPA

---

## 기능 목록

### 1. 카테고리 생성

사용자가 상품을 분류할 수 있는 카테고리를 새로 등록한다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/categories` |
| **요청 본문** | `{ "name": "string" }` |
| **응답** | 생성된 Category 객체 (id, name) |

---

### 2. 카테고리 목록 조회

등록된 모든 카테고리를 조회한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET /api/categories` |
| **응답** | Category 목록 |

---

### 3. 상품 등록

카테고리에 속하는 상품을 새로 등록한다. 이름, 가격, 이미지 URL, 카테고리를 지정한다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/products` |
| **요청 본문** | `{ "name": "string", "price": int, "imageUrl": "string", "categoryId": long }` |
| **응답** | 생성된 Product 객체 (id, name, price, imageUrl, category) |

---

### 4. 상품 목록 조회

등록된 모든 상품을 조회한다.

| 항목 | 내용 |
|------|------|
| **API** | `GET /api/products` |
| **응답** | Product 목록 |

---

### 5. 상품 옵션 등록

특정 상품에 대한 옵션(변형)을 등록한다. 옵션에는 이름과 재고 수량이 포함된다.

| 항목 | 내용 |
|------|------|
| **API** | OptionService를 통해 처리 |
| **요청 본문** | `{ "name": "string", "quantity": int, "productId": long }` |
| **응답** | 생성된 Option 객체 |

---

### 6. 위시리스트에 상품 추가

사용자가 원하는 상품을 자신의 위시리스트에 추가한다.

| 항목 | 내용 |
|------|------|
| **API** | WishService를 통해 처리 |
| **요청 본문** | `{ "productId": long }` |
| **동작** | 회원 ID와 상품 ID를 연결하여 Wish 엔티티 생성 |

---

### 7. 선물 보내기

사용자가 다른 회원에게 선물을 보낸다. 핵심 비즈니스 기능으로, 옵션 선택, 수량 지정, 메시지 작성이 포함된다.

| 항목 | 내용 |
|------|------|
| **API** | `POST /api/gifts` |
| **요청 헤더** | `Member-Id: long` (보내는 사람 식별) |
| **요청 본문** | `{ "optionId": long, "quantity": int, "receiverId": long, "message": "string" }` |
| **응답** | HTTP 200 |

**처리 흐름:**

1. 요청된 옵션 ID로 옵션 조회
2. 해당 옵션의 재고 수량 차감 (재고 부족 시 예외 발생)
3. 보내는 사람, 받는 사람, 옵션, 수량, 메시지를 포함한 Gift 객체 생성
4. GiftDelivery 구현체를 통해 선물 전달 처리 (현재는 콘솔 출력)

---

### 8. 재고 자동 관리

선물 보내기 실행 시 선택된 옵션의 재고 수량이 자동으로 차감된다. 재고가 부족할 경우 `IllegalStateException`이 발생하여 주문이 거부된다.

| 항목 | 내용 |
|------|------|
| **트리거** | 선물 보내기 API 호출 시 |
| **동작** | Option.decrease(quantity) 호출로 재고 차감 |
| **예외** | 요청 수량 > 잔여 수량일 때 예외 발생 |

---

## 외부 연동 (예정)

### 카카오톡 메시지 전송

카카오톡 API 연동을 위한 설정이 구성되어 있으나, 현재는 `FakeGiftDelivery`가 콘솔에 선물 정보를 출력하는 방식으로 동작한다.

| 항목 | 내용 |
|------|------|
| **메시지 API** | `https://kapi.kakao.com/v1/api/talk` |
| **설정 클래스** | `KakaoMessageProperties`, `KakaoSocialProperties` |
| **현재 구현** | `FakeGiftDelivery` (콘솔 출력) |

---

## 아키텍처

```
Client Request
    │
    ▼
REST Controller (ui/)
    │
    ▼
Service (application/)  ──▶  GiftDelivery (model/)
    │                              │
    ▼                              ▼
Repository (model/)        FakeGiftDelivery (infrastructure/)
    │
    ▼
H2 Database
```

- **트랜잭션:** 모든 Service에 `@Transactional` 적용
- **JPA:** `open-in-view=false` 설정으로 지연 로딩 범위를 서비스 계층으로 제한
- **확장성:** `GiftDelivery` 인터페이스를 통해 전달 방식을 교체 가능 (Strategy 패턴)
