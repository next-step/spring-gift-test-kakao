# Day 2 미션 복기

## 요구사항 1: Cucumber BDD

### 왜?
- 기존 RestAssured 테스트는 **개발자만 읽을 수 있다**
- Cucumber로 바꾸면 테스트가 곧 **비즈니스 문서**가 된다 (Living Documentation)
- 기획자, QA도 시나리오를 읽고 검증할 수 있다

### 구조 변화
- **Before**: 테스트 클래스 1개 = 시나리오 + 구현이 섞여있음
- **After**: `.feature`(시나리오) + `StepDefinitions`(구현) 분리

### 실행 흐름
1. `.feature` 파일의 한글 스텝 (`먼저`, `만일`, `그러면`)
2. → `@먼저`, `@만일`, `@그러면` 어노테이션이 붙은 Java 메서드에 매핑
3. → 각 메서드 안에서 RestAssured로 실제 HTTP 요청
4. → ScenarioContext로 스텝 간 데이터 공유 (categoryId 등)

### 핵심 개념
- **ScenarioContext + @ScenarioScope**: 시나리오 하나 동안만 살아있는 공유 저장소. 시나리오 끝나면 폐기.
- **Cucumber-Spring 통합**: `@CucumberContextConfiguration` + `@SpringBootTest`로 Spring 컨텍스트를 Cucumber에 연결
- **데이터 격리**: `@After` 훅에서 DB 정리. `@Transactional` 롤백이 안 되는 이유는 RestAssured가 별도 HTTP 요청이라 트랜잭션이 다르기 때문.

---

## 요구사항 2: PostgreSQL + Docker Compose

### 왜?
- H2는 인메모리 DB → 실제 운영 환경(PostgreSQL)과 동작이 다를 수 있다
- 예: SQL 문법 차이, 트랜잭션 격리 수준, 타입 처리 등
- **환경 패리티(Environment Parity)**: 테스트 환경을 운영 환경과 최대한 동일하게 맞춘다

### 구조 변화
- **Before**: H2 인메모리 → 앱 시작하면 자동으로 DB가 뜸
- **After**: Docker Compose로 PostgreSQL 컨테이너 실행 → 앱이 거기에 접속

### 실행 흐름
1. `docker compose up` → PostgreSQL 컨테이너 시작
2. healthcheck (`pg_isready`)로 DB 준비 완료 확인
3. Spring Boot 앱이 PostgreSQL에 접속 (`application-cucumber.properties`)
4. 테스트 실행
5. `docker compose down` → 컨테이너 정리

### 핵심 개념
- **Spring Profiles**: `@ActiveProfiles("cucumber")` → `application-cucumber.properties` 로드. 프로파일별로 DB 설정 분리.
- **Docker Compose healthcheck**: 컨테이너가 "시작됨"과 "준비됨"은 다르다. `pg_isready`로 DB가 실제로 쿼리 받을 준비가 되었는지 확인.
- **Gradle 태스크 자동화**: `cucumberTest`가 `doFirst`로 컨테이너 시작, `finalizedBy`로 종료. 한 명령어로 전체 라이프사이클 관리.

---

## 요구사항 3: 애플리케이션 컨테이너화

### 왜?
- 요구사항 2까지는 DB만 Docker, 앱은 로컬에서 실행
- 문제: "내 컴퓨터에서는 되는데?" — Java 버전, OS, 환경변수 차이로 다른 환경에서 실패 가능
- 앱까지 컨테이너에 넣으면 **누구나 동일한 환경**에서 테스트할 수 있다

### 구조 변화
- **Before**: 로컬 앱(`RANDOM_PORT`) + Docker PostgreSQL
- **After**: Docker 앱(고정 `28080`) + Docker PostgreSQL → 테스트 코드만 로컬

### 실행 흐름
1. `docker compose up` → PostgreSQL + Spring Boot 앱 컨테이너 모두 시작
2. 앱이 PostgreSQL에 접속 (Docker 네트워크 내부)
3. 테스트 코드가 `localhost:28080`으로 컨테이너 앱에 HTTP 요청
4. `docker compose down` → 전부 정리

### 핵심 개념
- **Multi-stage Docker build**: 빌드(JDK) → 실행(JRE) 분리. 이미지 크기 최소화.
- **WebEnvironment.NONE**: 앱이 이제 Docker에서 뜨니까, 테스트에서 로컬 서버를 띄울 필요가 없다. Spring 컨텍스트만 로드.
- **고정 포트 28080**: `RANDOM_PORT` 대신 Docker 포트매핑(`28080:8080`)에 맞춘 고정 포트.
- **depends_on + healthcheck**: 앱 컨테이너가 PostgreSQL이 준비된 후에 시작되도록 보장.

### 아직 남은 문제
- `curl http://localhost:28080`이 404 → 루트 `/` 경로에 매핑된 엔드포인트가 없음
- 검증용으로 `/api/categories` 같은 실제 엔드포인트를 쓰거나, Spring Boot Actuator의 `/actuator/health`를 추가하는 방법이 있다

---

## cucumberTest 전체 실행 흐름

`./gradlew cucumberTest` 한 줄이 실행되면 어떤 일이 벌어지는지, 처음부터 끝까지 추적한다.

### Phase 1: 인프라 기동 (Docker)

```
./gradlew cucumberTest
  └─ doFirst { docker compose up -d --wait }
       ├─ 1) postgres 컨테이너 시작 (PostgreSQL 17)
       │    └─ healthcheck: pg_isready -U test -d gift_test (3초 간격, 최대 10회)
       └─ 2) app 컨테이너 시작 (Spring Boot)
            ├─ depends_on: postgres (healthy 상태일 때만 시작)
            ├─ Multi-stage build: JDK로 빌드 → JRE로 실행
            └─ 포트 매핑: 28080(호스트) → 8080(컨테이너)
```

- `--wait` 플래그: 모든 컨테이너의 healthcheck가 통과할 때까지 기다림
- app 컨테이너는 환경변수로 DB 접속 정보를 주입받음 (`SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gift_test`)
- `postgres`는 Docker 내부 DNS 이름 — 같은 compose 네트워크 안에서 호스트명으로 통신

### Phase 2: Spring 컨텍스트 로드 (JUnit + Cucumber)

```
JUnit Platform
  └─ CucumberTest.java (@Suite, @IncludeEngines("cucumber"))
       ├─ @SelectClasspathResource("features")  → features/*.feature 파일 탐색
       └─ @ConfigurationParameter(GLUE = "gift.cucumber")  → 이 패키지에서 Step/Hook 스캔
            └─ CucumberSpringConfiguration.java
                 ├─ @CucumberContextConfiguration  → Cucumber가 Spring을 사용하겠다는 선언
                 ├─ @SpringBootTest(webEnvironment = NONE)  → 로컬 서버 안 띄움 (Docker에 이미 있으므로)
                 ├─ @ActiveProfiles("cucumber")  → application-cucumber.properties 로드
                 └─ @MockitoBean GiftDelivery  → 카카오 API 같은 외부 의존성 격리
```

**왜 `WebEnvironment.NONE`인가?**
- 앱은 이미 Docker 컨테이너(`localhost:28080`)에서 돌고 있다
- 테스트 프로세스에서 또 서버를 띄울 필요가 없다
- Spring 컨텍스트는 Repository 접근(DB 직접 검증)과 DI(ScenarioContext 등)를 위해서만 필요

### Phase 3: 시나리오 실행 (Feature → Step → HTTP)

하나의 시나리오가 실행되는 과정을 선물하기 예시로 추적한다.

```gherkin
# gift.feature
시나리오: 재고가 충분할 때 선물을 보내면 성공하고 재고가 차감된다

  먼저 "식품" 카테고리가 존재한다              ← (1)
  그리고 "식품" 카테고리에 4500원짜리
        "아메리카노" 상품이 존재한다           ← (2)
  그리고 "아메리카노" 상품에 재고 10개의
        "ICE" 옵션이 존재한다                 ← (3)
  그리고 "홍길동" 회원이 존재한다              ← (4)

  만일 "홍길동"이 "ICE" 옵션 3개를 선물한다    ← (5)
  그러면 선물 발송이 성공한다                  ← (6)
  그리고 "ICE" 옵션의 재고가 7개이다           ← (7)
```

각 스텝이 Java 메서드로 매핑되는 과정:

```
(1) @먼저("{string} 카테고리가 존재한다")  [CategoryStepDefinitions]
    → POST /api/categories {"name": "식품"}  (RestAssured → Docker 앱)
    → 응답에서 categoryId 추출 → context.set("categoryId:식품", 1L)

(2) @먼저("{string} 카테고리에 {int}원짜리 {string} 상품이 존재한다")  [ProductStepDefinitions]
    → context.get("categoryId:식품")으로 카테고리 ID 조회
    → POST /api/products {"name": "아메리카노", "price": 4500, "categoryId": 1}
    → context.set("productId:아메리카노", 1L)

(3) @먼저("{string} 상품에 재고 {int}개의 {string} 옵션이 존재한다")  [GiftStepDefinitions]
    → context.get("productId:아메리카노")
    → DB 직접 접근: optionRepository.save(new Option("ICE", 10, product))
    → context.set("optionId:ICE", 1L)

(4) @먼저("{string} 회원이 존재한다")  [GiftStepDefinitions]
    → DB 직접 접근: memberRepository.save(new Member("홍길동", "홍길동@test.com"))
    → context.set("memberId:홍길동", 1L)

(5) @만일("{string}이 {string} 옵션 {int}개를 선물한다")  [GiftStepDefinitions]
    → context에서 memberId, optionId 조회
    → POST /api/gifts
       Header: Member-Id: 1
       Body: {"optionId": 1, "quantity": 3, "receiverId": 2, "message": "생일 축하해!"}
    → context.set("lastResponse", response)

(6) @그러면("선물 발송이 성공한다")  [GiftStepDefinitions]
    → context.get("lastResponse") → response.then().statusCode(200)

(7) @그리고("{string} 옵션의 재고가 {int}개이다")  [GiftStepDefinitions]
    → DB 직접 접근: optionRepository.findById(optionId)
    → assertThat(option.getQuantity()).isEqualTo(7)  ← 10 - 3 = 7 확인
```

### Phase 4: 시나리오 간 격리 (ScenarioHooks)

```
[시나리오 시작]
  └─ @Before: RestAssured.port = 28080  (Docker 앱 포트)

[시나리오 실행 - 위의 (1)~(7)]

[시나리오 종료]
  └─ @After: 테이블 정리 (외래키 순서 중요!)
       wishRepository.deleteAllInBatch()      ← 자식부터
       optionRepository.deleteAllInBatch()
       productRepository.deleteAllInBatch()
       categoryRepository.deleteAllInBatch()
       memberRepository.deleteAllInBatch()    ← 부모 마지막
```

**왜 `@Transactional` 롤백이 안 되는가?**
```
[테스트 프로세스]                    [Docker 앱 컨테이너]
  RestAssured ──HTTP 요청──→  컨트롤러 → 서비스 → DB
  (트랜잭션 A)                       (트랜잭션 B)
```
- 테스트와 앱이 **다른 프로세스**에서 실행된다
- 트랜잭션이 완전히 별개이므로 테스트에서 롤백해도 앱이 커밋한 데이터는 남는다
- 그래서 `@After`에서 직접 `deleteAllInBatch()`로 정리하는 것

### Phase 5: ScenarioContext의 생명주기

```
ScenarioContext (@Component + @ScenarioScope)
  └─ 내부: Map<String, Object>

시나리오 1 시작 → 새 ScenarioContext 인스턴스 생성
  set("categoryId:식품", 1L)
  set("productId:아메리카노", 1L)
  set("lastResponse", response)
시나리오 1 종료 → ScenarioContext 폐기 (GC 대상)

시나리오 2 시작 → 또 다른 새 ScenarioContext 인스턴스 생성
  (이전 시나리오의 데이터는 없음 — 완전히 깨끗한 상태)
시나리오 2 종료 → 폐기
```

- `@ScenarioScope`: Cucumber 시나리오 하나의 수명과 동일한 Spring Bean 스코프
- 시나리오 내의 모든 StepDefinitions 클래스가 **같은 인스턴스**를 `@Autowired`로 주입받음
- 이를 통해 `먼저` 스텝에서 만든 ID를 `만일`/`그러면` 스텝에서 사용할 수 있음

### Phase 6: 정리 (Docker Down)

```
cucumberTest 태스크 완료 (성공이든 실패든)
  └─ finalizedBy dockerDown
       └─ docker compose down
            ├─ app 컨테이너 제거
            ├─ postgres 컨테이너 제거
            └─ 네트워크 제거
```

- `finalizedBy`: 테스트가 실패하더라도 **반드시** 실행됨 (try-finally와 같은 역할)
- 컨테이너가 남아있으면 포트 충돌이나 데이터 오염이 발생할 수 있으므로 항상 정리

### 한눈에 보기: 전체 아키텍처

```
┌─ 로컬 (테스트 프로세스) ──────────────────────────────┐
│                                                       │
│  JUnit → Cucumber Engine → Feature 파일 파싱          │
│    ↓                                                  │
│  StepDefinitions (Java 메서드)                        │
│    ├─ RestAssured ──HTTP──→ localhost:28080 ─────┐    │
│    ├─ ScenarioContext (스텝 간 데이터 공유)        │    │
│    └─ Repository (DB 직접 접근으로 검증)           │    │
│                                                  │    │
│  ┌─ Docker Compose ─────────────────────────┐    │    │
│  │                                          │    │    │
│  │  ┌─ app 컨테이너 ──────────────────┐     │    │    │
│  │  │  Spring Boot (port 8080)        │←────┘    │    │
│  │  │    ↓                            │          │    │
│  │  │  Controller → Service → JPA     │          │    │
│  │  │    ↓                            │          │    │
│  │  └────┼────────────────────────────┘          │    │
│  │       │ jdbc:postgresql://postgres:5432       │    │
│  │  ┌────↓────────────────────────────┐          │    │
│  │  │  postgres 컨테이너              │          │    │
│  │  │  PostgreSQL 17 (port 5432)      │←─────────┘    │
│  │  │  DB: gift_test                  │  (직접 접근)   │
│  │  └─────────────────────────────────┘               │
│  └────────────────────────────────────────────┘       │
└───────────────────────────────────────────────────────┘
```

**데이터 흐름 두 가지 경로:**
1. **HTTP 경로** (인수테스트): RestAssured → Docker 앱 → DB (사용자 행위 시뮬레이션)
2. **직접 접근 경로** (데이터 준비/검증): Repository → DB (테스트 편의를 위한 바로가기)
