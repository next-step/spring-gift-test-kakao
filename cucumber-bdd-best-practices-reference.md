# Cucumber 사용 가이드 (Feature 설계 & 운영 Best Practices)

> 목적: 이 문서는 **Claude Code / Codex**가 작업 중 참조할 수 있도록, Cucumber(Gherkin) 기반 BDD 테스트를 **읽기 쉽고 유지보수 가능하게** 만드는 실무 규칙을 정리합니다.  
> 초점: “테스트 자동화 도구로서의 Cucumber”가 아니라 **도메인 행동(Behavior)을 살아있는 문서로 유지**하는 방법.

---

## 0. Cucumber/BDD의 핵심 원칙

Cucumber는 *사람이 읽을 수 있는 예시(Examples)*를 Gherkin으로 작성하고, 이를 자동화 코드(steps)로 연결해 **요구사항 ↔ 구현**을 동기화합니다.  
좋은 BDD는 대체로 다음 사이클로 돌아갑니다:

- **Discovery**: 사용자/도메인 관점에서 구체적 예시를 합의
- **Formulation**: 예시를 Gherkin(Feature/Scenario)으로 정제
- **Automation**: 자동화(steps)로 구현해 지속적으로 검증

---

## 1. Feature 파일 설계 규칙 (가독성/도메인 중심)

### 1.1 Feature는 “사용자 가치” 단위로
- Feature 제목은 *기능 구현 단위*가 아니라 **사용자/비즈니스 가치**를 표현
- 예: “카테고리 CRUD” 대신 “관리자가 카테고리를 관리할 수 있다”

권장 템플릿:
```gherkin
Feature: <도메인/사용자 가치>

  As a <역할>
  I want <목표>
  So that <가치/이유>
```

### 1.2 Scenario는 짧고 한 가지 규칙만 검증
- 한 시나리오는 **한 가지 비즈니스 규칙**만 다루도록 분리
- 길어지면 Scenario를 쪼개거나 “테스트 데이터 준비”를 재사용 가능한 헬퍼로 옮기기

### 1.3 Given/When/Then 역할을 흐리지 않기
- Given: 전제조건(상태)
- When: 행동(트리거)
- Then: 기대결과(관찰 가능한 결과)
- And/But: 같은 문맥의 추가 조건/결과

규칙:
- When은 보통 **1개** (행동은 하나로)
- Then은 복수여도 되지만, **같은 결과 영역**(예: 응답/DB/이벤트)에서만 묶기

---

## 2. 시나리오 작성 Best Practices

### 2.1 명령형(implementation-heavy) 대신 선언형(behavior-focused)
❌ UI 클릭/세부 구현이 드러나는 문장:
```gherkin
When 제출 버튼을 클릭한다
Then #success-toast 요소가 보여야 한다
```

✅ 사용자 행동/도메인 결과 중심:
```gherkin
When 관리자가 "전자기기"라는 카테고리를 생성한다
Then 카테고리 목록에 "전자기기"가 포함되어 있다
```

### 2.2 “검증 가능한 관찰”로 Then을 쓴다
- Then은 내부 구현이 아니라 **관찰 가능한 결과**로 표현
- 예: API 응답, 상태 코드, DB에 저장된 엔티티, 발행된 이벤트, 화면에 보이는 메시지(필요 시)

### 2.3 Scenario Outline로 데이터 기반 테스트를 명확히
- 같은 규칙을 여러 입력으로 검증해야 하면 Outline 사용
- 예: 이름 길이/금지문자/중복 등

```gherkin
Scenario Outline: 카테고리 이름 검증
  When 관리자가 "<name>"이라는 카테고리를 생성한다
  Then "<reason>" 사유로 요청이 거부된다

Examples:
  | name       | reason             |
  |            | 이름이 비어있음     |
  | "   "      | 이름이 비어있음     |
  | "A"*101    | 이름이 너무 김      |
```

### 2.4 Background는 “정말 공통인 전제”만
- Background가 길어지면 시나리오가 읽기 어려워지고 디버깅도 어려움
- 공통 전제가 많다면:
  - `Given 관리자가 로그인한 상태이다` 같은 **하나의 고수준 step**으로 축약하거나
  - 훅/테스트 픽스처로 이동

---

## 3. Step Definitions 설계 규칙 (유지보수의 핵심)

### 3.1 Step는 “도메인 언어(ubiquitous language)”로
- step 이름은 도메인 용어로 고정: 팀 합의된 용어집(Glossary)와 일치
- 같은 의미를 여러 표현으로 만들지 않기(중복 step 폭발 방지)

### 3.2 Step는 “얇게(thin)” 유지하고 로직은 헬퍼/서비스로
권장 구조:
- step: 파라미터 파싱/컨텍스트 저장/헬퍼 호출
- helper/service: 실제 HTTP 호출, DB 준비, 검증 로직

### 3.3 재사용성 높은 파라미터화
- 하드코딩 대신 파라미터 사용
- 데이터 테이블(DataTable)로 입력 구조화

```gherkin
When 관리자가 다음 카테고리들을 생성한다:
  | name     |
  | 전자기기 |
  | 식품     |
```

### 3.4 테스트 컨텍스트(World/Scenario Context)를 명확히
- 시나리오마다 독립적인 컨텍스트를 사용 (전역 상태 최소화)
- “마지막 응답(lastResponse)”, “생성된 ID(createdId)” 등 *읽기 쉬운 이름* 사용

---

## 4. Hooks & Test Isolation (플래키 방지)

### 4.1 격리(Isolation) 원칙
- 시나리오 간에 DB/캐시/큐/파일 시스템 상태가 섞이지 않게
- 가장 흔한 방식:
  - 시나리오마다 트랜잭션 롤백
  - 테스트 전용 DB 스키마/컨테이너(Testcontainers 등)
  - 각 시나리오에서 생성한 데이터만 정리(태그 기반)

### 4.2 Hooks는 “인프라/환경”에만
- `@Before`/`@After`는 로그인, DB 초기화, 테스트 서버 준비 같은 **환경 레벨**에만 사용
- 비즈니스 규칙은 Scenario 본문에서 읽히게 유지

### 4.3 Tags로 실행 범위를 관리
- 예: `@smoke`, `@regression`, `@api`, `@ui`, `@wip`
- CI 파이프라인 단계에 따라 필요한 태그만 실행
- 오래 걸리는 UI 시나리오는 별도 태그로 분리

---

## 5. 폴더/네이밍 컨벤션(추천)

예시(언어 무관, 개념 중심):
```
features/
  카테고리/
    카테고리_목록_조회.feature
    카테고리_생성.feature
steps/
  카테고리_steps.*
support/
  hooks.*
  world.*
  api_client.*
  db_fixtures.*
```

규칙:
- feature 파일명은 **행동 + 대상**으로 짓기: `카테고리_생성.feature`
- step 파일은 도메인 단위로 묶기: `카테고리_steps`
- 공통 헬퍼는 `support/`로 이동

---

## 6. Anti-pattern 모음 (자주 망가지는 지점)

- 시나리오가 “스크립트”가 됨: 클릭/입력/대기 같은 UI 절차가 대부분
- Given에 When이 섞임: “Given I click …”
- Then이 구현 세부를 검증: 내부 함수 호출 여부, 특정 클래스 이름 등
- Step 중복이 폭발: 같은 의미를 다른 문장으로 계속 추가
- Background가 과도하게 커져서 시나리오가 읽히지 않음
- 테스트 데이터가 전역 공유되어 순서 의존/플래키 발생

---

## 7. 빠르게 시작하는 템플릿 (API 중심 예시)

```gherkin
Feature: 카테고리 관리

  관리자로서
  카테고리를 관리하고 싶다
  상품을 체계적으로 분류할 수 있도록

  Scenario: 카테고리가 없을 때 목록 조회
    Given 카테고리가 존재하지 않는다
    When 관리자가 카테고리 목록을 조회한다
    Then 응답 상태 코드는 200이다
    And 카테고리 목록은 비어있다

  Scenario: 카테고리 생성
    When 관리자가 "전자기기"라는 카테고리를 생성한다
    Then 응답 상태 코드는 201이다
    And 응답에 "전자기기" 카테고리가 포함되어 있다
    And 데이터베이스에 "전자기기" 카테고리가 존재한다
```

Step 구현 팁:
- HTTP 호출은 `ApiClient` 같은 헬퍼로 통일
- DB 검증도 `Repository/DAO`를 직접 쓰기보다 “검증 헬퍼”로 추상화(읽기 쉬움 유지)

---

## 8. Claude Code / Codex에게 시킬 때 프롬프트 체크리스트

에이전트에게 작업을 요청할 때 아래를 같이 제공하면 결과 품질이 좋아집니다.

- [ ] Feature/Scenario가 검증해야 할 **비즈니스 규칙** 1~2줄 요약
- [ ] 도메인 용어(카테고리/옵션/상품 등) 정의
- [ ] 성공/실패 기준(상태 코드, 메시지, DB 상태)
- [ ] 실행 명령(테스트/린트) 및 환경(테스트 DB, 컨테이너)
- [ ] 기존 step 재사용 우선(새 step 만들기 전 검색)

---

## 참고자료 (원문 링크)

아래 URL은 참고용입니다(이 문서 자체는 팀 내부 레퍼런스 용도로 재구성).

```text
https://cucumber.io/docs/bdd/
https://dev.to/maria_bueno/testing-framework-cucumber-best-practices-for-bdd-testing-4d82
https://www.geeksforgeeks.org/software-testing/cucumber-best-practices/
https://apidog.com/blog/cucumber-bdd-testing/
```
