# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 빌드 및 테스트 명령어

```bash
./gradlew build          # 빌드 + 테스트
./gradlew test           # 전체 테스트 실행
./gradlew bootRun        # 애플리케이션 실행 (H2 인메모리 DB, 포트 8080)
./gradlew test --tests "gift.SomeTest"              # 단일 테스트 클래스 실행
./gradlew test --tests "gift.SomeTest.methodName"   # 단일 테스트 메서드 실행
```

Java 21 필수.

## 현재 과제: 1단계 — 레거시 코드 인수 테스트

이 프로젝트의 1단계 과제는 레거시 코드에 대한 **인수 테스트(Acceptance Test)** 작성이다.

### 제출물
1. **TEST_STRATEGY.md** — 검증할 행위 목록, 테스트 데이터 전략, 검증 전략, 주요 의사결정
2. **테스트 코드** — 최소 5개 이상의 **행위(behavior)** 를 검증 (테스트 메서드 5개가 아닌 행위 5개)
3. **AI 활용 문서** — 프롬프트 및 접근 방법 정리

### 제약 조건
- BDD 도구(Cucumber, Karate 등) 사용 금지
- 사용자 관점에서 행위를 검증하는 테스트 (API 레벨의 인수 테스트)

### 테스트 설계 원칙: "어떻게 되는가"를 검증한다

인수 테스트는 **내부 구현이 아닌 사용자 입력과 그 결과**에 의존해야 한다. 세부 구현(엔티티 구조, repository 메서드, 서비스 내부 로직)에 의존하는 테스트는 리팩토링 시 깨진다. "어떻게 하는가"가 아니라 **"어떻게 되는가"**를 검증하는 테스트를 작성한다.

예시: 재고 차감 검증
- **나쁨** (구현 의존): 선물 후 `optionRepository.findById()`로 quantity 직접 조회
- **좋음** (행위 검증): 재고 전부 소진하는 선물 → 성공 / 같은 옵션에 추가 선물 → 재고 부족으로 실패

### 테스트 작성 가이드

#### 기술 스택
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + **RestAssured**
- H2 인메모리 DB 사용 (별도 설정 불필요)
- RestAssured 의존성 필요: `testImplementation 'io.rest-assured:rest-assured'`

```java
// RestAssured 사용 예시
ExtractableResponse<Response> response = RestAssured.given().log().all()
        .contentType(ContentType.JSON)
        .header("Accept", "application/json")
        .header("Member-Id", 1)
        .body(createGiftRequest(1L, 1, 2L, "생일 축하"))
        .when().post("/api/gifts")
        .then().log().all().extract();
```

#### 컨트롤러별 요청 바인딩 주의
- `POST /api/products`, `POST /api/categories` → **`@RequestBody` 없음 → form params**로 전송
- `POST /api/gifts` → **`@RequestBody` 있음 → JSON body**로 전송

#### 테스트 데이터 전략: @Sql 스크립트
- repository를 직접 사용하면 Java 엔티티/생성자에 의존 → 리팩토링 시 깨짐
- **@Sql 스크립트**로 데이터를 준비하고 정리한다 (구현 비의존)
- `src/test/resources/`에 SQL 파일 배치

```java
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
```

- cleanup.sql → 모든 테이블 TRUNCATE (FK 순서 고려)
- test-data.sql → 테스트에 필요한 기본 데이터 INSERT
- H2의 컬럼명은 JPA 네이밍 전략을 따름 (예: `imageUrl` → `image_url`)

#### 테스트 격리
- **`RANDOM_PORT`에서 `@Transactional` 롤백은 동작하지 않는다.** 실제 HTTP 요청은 별도 스레드에서 처리되므로 테스트 트랜잭션과 분리됨.
- 매 테스트 전 `@Sql`로 cleanup → 데이터 재세팅하여 격리 보장

#### 검증 전략: 다음 행동으로 이전 행동을 검증
- DB를 직접 조회하지 않고 **API 응답**과 **후속 행위의 성공/실패**로 검증
- 카테고리 생성 → 해당 카테고리로 상품 생성 → 상품 목록 조회에서 카테고리 확인 (시나리오 체이닝)
- 재고 전부 소진하는 선물 → 성공 → 같은 옵션에 재선물 → 실패 (재고 감소 검증)

### 검증 대상 핵심 행위
1. 카테고리 생성 (`POST /api/categories`) — 응답에 id, name 확인
2. 카테고리 목록 조회 (`GET /api/categories`) — 생성한 카테고리가 목록에 존재
3. 상품 생성 (`POST /api/products`) — 응답에 id, name, category 확인
4. 상품 목록 조회 (`GET /api/products`) — 생성한 상품이 목록에 존재
5. 선물하기 성공 (`POST /api/gifts`) — 재고 충분 시 200 응답
6. 선물하기 후 재고 감소 — 재고 전부 소진 후 재시도 시 실패로 검증 (행위 기반)
7. 재고 부족 시 선물 실패 — 재고 초과 수량 요청 시 500 응답

> **WishService**: 컨트롤러가 없으므로 API 레벨 인수 테스트 범위에서 제외. 필요 시 서비스 레벨 테스트로 별도 분리 가능하나, "사용자 관점 행위 검증" 취지와 맞지 않음.

## 아키텍처

선물하기 플랫폼 (카카오 선물하기 스타일). Spring Boot 3.5, JPA + H2, Thymeleaf.

### 패키지 구조 (`src/main/java/gift/`)

- **model/** — JPA 엔티티(`Product`, `Category`, `Option`, `Member`, `Wish`), 리포지토리, 도메인 인터페이스(`GiftDelivery`). `Gift`는 선물 트랜잭션에서만 사용되는 비영속 값 객체.
- **application/** — 서비스 및 요청 DTO. 모든 서비스는 `@Transactional` + 생성자 주입.
- **ui/** — REST 컨트롤러 (`/api/products`, `/api/categories`, `/api/gifts`).
- **infrastructure/** — 인터페이스 구현체 및 외부 설정 프로퍼티. `FakeGiftDelivery`는 현재 `GiftDelivery` 구현체 (콘솔 출력 스텁).

### 핵심 도메인 관계

```
Category 1──N Product 1──N Option
Member 1──N Wish N──1 Product
Member(발신자) + Member(수신자) + Option → Gift (비영속)
```

### 선물하기 흐름 (핵심 비즈니스 로직)

`POST /api/gifts` + `Member-Id` 헤더 → `GiftService.give()` → `Option` 조회, `option.decrease(quantity)` 호출 (재고 부족 시 `IllegalStateException`), `Gift` 값 객체 생성, `GiftDelivery.deliver()` 위임. 재고는 트랜잭션 내 JPA dirty checking으로 자동 반영.

### 확장 포인트

`GiftDelivery` 인터페이스(`model/`)는 선물 배송 전략 패턴. 현재는 `FakeGiftDelivery`(콘솔 출력). 향후 `KakaoMessageProperties` / `KakaoSocialProperties` 설정(`kakao.message.*`, `kakao.social.*`)을 사용한 카카오 API 구현체로 교체 예정.

### 주요 컨벤션

- `open-in-view=false` — 트랜잭션 밖에서 지연 로딩 불가.
- 인증 계층 미구현. 발신자 식별은 `Member-Id` 요청 헤더로 처리.
- `WishService`는 존재하나 대응 컨트롤러 없음.
