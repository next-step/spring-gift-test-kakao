## 테스트 전략
1. 최소 5개 이상의 행위를 검증하는 테스트를 작성해야 한다.
   - 최소 5개의 행위를 검증하는 것이지 테스트가 5개라는 말은 아니다.
2. 검증할 행위의 기준: API 엔드포인트
   - API 스펙은 직군 관계없이 모두가 파악 가능하고, 구현된 코드에 비해 잘 변경되지 않기 때문이다.
   - 엔드포인트별로 '행위'가 명확하게 구분되기 때문이다.
3. BDD(Cucumber + Gherkin)로 인수 테스트를 작성한다.
   - 모두가 읽을 수 있는 테스트, 모두가 합의하고 자동화할 수 있는 테스트를 만들기 위함이다.
   - Gherkin `.feature` 파일로 비즈니스 시나리오를 기술하고, Step Definitions(Java)로 자동화한다.
4. Gherkin은 도메인 언어로 표현한다.
   - 기술 용어(HTTP, JSON, API, 200, 500) 대신 비즈니스 용어를 사용한다.
   - 구현이 바뀌어도 Gherkin 시나리오는 유지되어야 하기 때문이다.
   ```gherkin
   # 좋은 예 — 비즈니스 언어
   When 회원 1번이 "아이폰" 1개를 선물한다
   Then 선물 발송이 성공한다

   # 나쁜 예 — 기술 용어
   When POST /api/gifts with body {"optionId": 1, "quantity": 1}
   Then response status is 200
   ```
5. Gherkin의 추상화 수준은 균형을 잡는다.
   - 너무 구체적이면 재사용할 수 없고, 너무 일반적이면 읽을 수 없기 때문이다.
   ```gherkin
   # 균형 잡힌 수준
   Given "ICE" 옵션의 재고가 10개 있다

   # 너무 구체적
   Given Option 테이블에 id=1, name=ICE, quantity=10 레코드를 INSERT 한다

   # 너무 일반적
   Given 데이터가 준비되어 있다
   ```
6. Step 간 상태 공유는 ScenarioContext를 사용한다.
   - Given에서 생성한 ID를 When에서 사용하고, When의 응답을 Then에서 검증해야 하기 때문이다.
   - Step Definition 클래스에 직접 상태를 저장하면 시나리오 간 격리가 깨지기 때문이다.
   ```java
   // Step Definition에 상태 저장하지 않는다
   private Long optionId;

   // ScenarioContext를 사용한다
   context.set("optionId", optionId);
   ```
7. 외부 의존은 Test Double로 격리한다.
   - `GiftDelivery`처럼 실제 배송 시스템일 수 있는 의존은 테스트 환경에서 직접 호출할 수 없기 때문이다.
   - Cucumber 인수 테스트에서는 Fake를, 실패 시나리오에서는 Stub을, 단위 테스트에서는 Mock을 사용한다.
8. 리스크 기반으로 자동화 대상을 선정한다.
   - 모든 기능을 자동화할 수 없으므로, `Risk = Impact x Likelihood x Detection Cost`로 우선순위를 정한다.
   - High Risk는 Cucumber로 자동화하고, Low Risk는 수동 QA로 충분하다.
9. HTTP 계약, 관찰 가능한 상태 결과, 그리고 사이드 이펙트를 검증한다.
   - 인수테스트의 핵심은 '외부에서 관찰 가능한 결과'를 검증하는 것이기 때문이다.
   - DB 구조 및 코드가 바뀌어도 API 계약이 유지되는 한 테스트는 유효하기 때문이다.
   - DB를 직접 조회하기보다 API를 통해 확인해야 실질적인 비즈니스 요구사항을 만족하는지 여부를 확인할 수 있기 때문이다.
10. 각 시나리오마다 DB를 초기화한다.
    - 테스트 간 데이터가 간섭하면 실행 순서에 따라 결과가 달라지기 때문이다.
    - Cucumber의 `@Before` 훅에서 전체 테이블을 truncate하는 방식으로 격리한다.
11. 테스트 DB는 Docker Compose로 PostgreSQL을 사용한다.
    - H2 in-memory는 프로덕션과 SQL 방언, flush 동작, 트랜잭션 동작이 다르기 때문이다.
    - Spring 프로파일로 테스트/개발 DB를 분리하고, 테스트 실행 시 PostgreSQL을 자동으로 시작한다.
12. 애플리케이션도 Docker 컨테이너로 실행할 수 있어야 한다.
    - 프로덕션과 완전히 동일한 환경에서 E2E 테스트를 수행하기 위함이다.
    - Dockerfile은 Multi-stage build로 작성하고, Docker Compose에 애플리케이션 서비스를 추가한다.
13. 인수 테스트는 단일 커맨드로 실행할 수 있어야 한다.
    - `./gradlew cucumberTest` 한 번으로 PostgreSQL이 자동 준비되고 전체 Cucumber 시나리오가 실행되어야 한다.
    - Docker 기반 E2E는 `./gradlew dockerBuild` → `./gradlew dockerUp` → `./gradlew cucumberTest` → `./gradlew dockerDown` 순서로 실행한다.
    - CI(GitHub Actions)에서도 동일한 커맨드로 자동 실행되어야 한다.