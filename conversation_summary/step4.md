## 대화 요약

### 요약 주제
테스트 인프라 개선 (H2 전환, 설정 정리) 및 Feature 파일 비개발자 가독성 개선

### 핵심 내용
- 피어 리뷰(testrace)의 피드백을 반영하여 `test` 태스크의 Docker 의존성을 제거하고 H2로 전환함
- PostgreSQL 설정을 `application.properties`에 기본값으로 명시하고 `docker-compose.yml`의 중복 환경변수를 정리함
- Feature 파일에서 HTTP 상태 코드, 기술 용어를 제거하고 비즈니스 언어로 교체함
- README.md에 `criteria/`, `conversation_summary/` 문서 링크를 추가함

---

### 1단계: Application 레이어 테스트 전략 논의

#### 질문: DB를 거치는 통합 테스트 vs Mock 단위 테스트?

**결론: DB를 거치는 통합 테스트가 적합**

| 근거 | 설명 |
|------|------|
| 서비스가 얇음 | 전부 `findById → 생성 → save` 패턴, Mock하면 구현 검증이 됨 |
| 표준 JPA만 사용 | PostgreSQL 고유 기능 없음 |
| 행위 중심 테스트 | "상품이 실제로 생성되었나?"를 검증해야 함 |
| 예외: 외부 인프라 | `GiftDelivery`는 `FakeGiftDelivery`로 이미 격리됨 |

#### H2의 ACID 지원 여부

- H2는 ACID 완전 지원 (RDBMS)
- `@Transactional` 롤백은 DB 종류와 무관하게 동작
- 단, 동시성(locking 전략), 격리 수준 세부 동작, DDL 트랜잭션은 PostgreSQL과 차이 있음
- 현재 테스트는 단일 스레드 기본 롤백 검증이므로 H2로 충분

---

### 2단계: test 태스크 Docker 의존성 제거 (피어 리뷰 반영)

#### 피어 피드백 (testrace)
> "로컬의 빠른 피드백 대신 Production Parity를 선택하신 것 같아요. 빠른 단위 테스트와 Production Parity는 어떤 기준으로 분리하려고 하셨나요?"

#### 분리 기준

| 기준 | H2 (빠른 피드백) | PostgreSQL (Production Parity) |
|------|:-:|:-:|
| 표준 JPA만 사용 | O | |
| Native SQL / DB 고유 함수 | | O |
| DB별 동작 차이 (locking, isolation) | | O |
| 개발 중 반복 실행 | O | |
| CI 최종 검증 | | O |

#### 변경 사항

**`build.gradle`**: `test` 태스크에서 Docker 의존성 제거
```diff
 tasks.named('test') {
-    dependsOn 'startDb'
-    finalizedBy 'stopDb'
     useJUnitPlatform()
     exclude '**/acceptance/**'
 }
```

**`application-test.properties`**: PostgreSQL → H2 전환
```diff
-spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
-spring.datasource.username=test
-spring.datasource.password=test
-spring.datasource.driver-class-name=org.postgresql.Driver
-spring.jpa.hibernate.ddl-auto=create
-spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
+spring.datasource.url=jdbc:h2:mem:gift_test;DB_CLOSE_DELAY=-1
+spring.datasource.driver-class-name=org.h2.Driver
+spring.jpa.hibernate.ddl-auto=create
```

---

### 3단계: PostgreSQL 설정 가시성 확보

#### 문제
- `application.properties`에 DB 설정이 없어서, Spring 설정 파일만 봐서는 이 프로젝트가 PostgreSQL을 사용하는지 알 수 없었음

#### 변경: `application.properties`에 기본값 명시

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gift_test
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=create
```

#### docker-compose.yml 환경변수 정리

```diff
 environment:
   SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gift_test
-  SPRING_DATASOURCE_USERNAME: test
-  SPRING_DATASOURCE_PASSWORD: test
-  SPRING_JPA_HIBERNATE_DDL_AUTO: create
```

- `SPRING_DATASOURCE_URL`만 유지: Docker 내부 DNS(`postgres`)와 로컬(`localhost`)의 호스트가 다르기 때문
- username, password, ddl-auto는 `application.properties` 기본값과 동일하므로 제거

#### 최종 설정 흐름

```
application.properties        ← PostgreSQL 기본값 (localhost)
  ↑ test 프로필이 override
application-test.properties   ← H2 (Docker 불필요)
  ↑ Docker에서 URL만 override
docker-compose.yml            ← 호스트만 postgres로 변경
```

---

### 4단계: Feature 파일 비개발자 가독성 개선 (피어 리뷰 반영)

#### 피어 피드백 (testrace)
> "'응답 상태 코드 200' 보다 비개발자가 쉽게 이해할 수 있는 표현은 어떨까요? 이미 의도가 충분히 드러난다면 제거해도 괜찮지 않을까요?"

#### 변경 1: 성공 시나리오에서 상태 코드 제거

의도가 결과 검증으로 이미 드러나므로 제거함.

```diff
 Scenario: 선물 전송 성공
   When 보내는 회원이 3개 수량으로 선물을 전송하면
-  Then 응답 상태 코드는 200이다
-  And 옵션 재고가 7개로 차감되어 있다
+  Then 옵션 재고가 7개로 차감되어 있다
```

- 3개 feature 파일에서 총 9곳의 `응답 상태 코드는 200이다`를 제거

#### 변경 2: 실패 시나리오를 비즈니스 언어로 교체

```diff
-  Then 응답 상태 코드는 500이다
+  Then 선물 전송에 실패한다

-  Then 응답 상태 코드는 500이다
+  Then 상품 생성에 실패한다
```

#### 변경 3: 기술 용어 제거

```diff
-  Scenario: Member-Id 헤더 없이 선물 전송 시 실패한다
-    When Member-Id 헤더 없이 선물을 전송하면
-    Then 응답 상태 코드는 400이다
+  Scenario: 로그인하지 않은 사용자는 선물을 전송할 수 없다
+    When 로그인하지 않은 사용자가 선물을 전송하면
+    Then 선물 전송에 실패한다
```

#### Step Definition 변경

| 파일 | 변경 |
|------|------|
| `CommonSteps` | `응답_상태_코드는` 스텝 제거 |
| `GiftSteps` | `선물 전송에 실패한다` 스텝 추가, `Member-Id 헤더 없이` → `로그인하지 않은 사용자가` 변경 |
| `ProductSteps` | `상품 생성에 실패한다` 스텝 추가 |

#### 기술 디테일 기록 위치

Feature 파일은 비개발자용으로 깔끔하게 유지하고, step definition에 주석으로 기술 디테일을 기록함.

```java
// HTTP 4xx/5xx 응답을 실패로 판단 (재고 부족: 500, 인증 실패: 400, 옵션 미존재: 500)
@Then("선물 전송에 실패한다")
public void 선물_전송에_실패한다() {
    assertThat(context.getResponse().statusCode()).isGreaterThanOrEqualTo(400);
}

// Member-Id 헤더 없이 요청하여 인증되지 않은 사용자를 시뮬레이션
@When("로그인하지 않은 사용자가 선물을 전송하면")
```

---

### 5단계: README.md 문서 링크 추가

`criteria/`와 `conversation_summary/` 문서를 README에서 바로 찾을 수 있도록 링크를 추가함.

---

### 결정사항

| 항목 | 선택 | 대안 | 선택 이유 |
|------|------|------|----------|
| Application 테스트 DB | H2 in-memory | Docker PostgreSQL | 표준 JPA만 사용, Docker 없이 빠른 피드백 |
| PostgreSQL 설정 위치 | `application.properties` 기본값 | `docker-compose.yml` 환경변수만 | 설정 가시성 확보, properties만 봐도 DB 스택 파악 가능 |
| Docker URL override | `SPRING_DATASOURCE_URL`만 유지 | 전체 환경변수 유지 | 호스트만 다르고 나머지는 기본값과 동일 |
| 성공 시나리오 상태 코드 | 제거 | 유지 | 결과 검증으로 의도가 이미 드러남 |
| 실패 표현 | 비즈니스 언어 (`실패한다`) | HTTP 상태 코드 (`500이다`) | 비개발자 가독성 |
| 기술 디테일 기록 | Step definition 주석 | Feature 파일 주석 | Feature는 비즈니스 문서, Step은 개발자 문서 |
| 테스트 실행 구조 | `test`: H2, `cucumberTest`: Docker | 전부 Docker | 빠른 피드백과 Production Parity 분리 |
