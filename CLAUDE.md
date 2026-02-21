# CLAUDE.md
# Project: Legacy Code Acceptance Tests (Behavior-Safety Net)

이 프로젝트의 목표는 **테스트가 없는 레거시 코드**에서 리팩터링을 안전하게 수행할 수 있도록,
**사용자 관점의 External Behavior(외부 행동)** 을 고정하는 테스트(인수/행동 기반)를 작성하는 것입니다.

핵심 원칙:
- "Legacy code is code without tests." (Michael Feathers)
- "Refactoring is restructuring without changing external behavior." (Martin Fowler)
- 보호 대상은 내부 호출/구현이 아니라 **외부 행동의 결과** 입니다.


## What to Deliver (Required Outputs)

아래 3가지를 반드시 제출물로 남깁니다.

1) `TEST_STRATEGY.md`
- 검증할 행위 목록: 어떤 행위를 선택했는가? 기준은?
- 테스트 데이터 전략: 어떻게 준비/정리하는가?
- 검증 전략: 무엇을 어떻게 검증하는가?
- 주요 의사결정: 페어 논의 중 중요한 선택과 이유

2) 테스트 코드
- **최소 5개 이상의 “행위(behavior)”** 를 검증해야 함
    - 테스트 파일이 5개라는 뜻이 아니라, “행위”가 5개 이상 고정되어야 함

3) `AI_USAGE.md` (또는 `PROMPTS.md`)
- Claude Code(또는 AI 도구) 활용 방법 문서화
- 실제 사용한 프롬프트/접근 방법/정의한 스킬 포함


## Non-goals / Constraints

- BDD 도구(예: Cucumber, Karate 등) 사용 금지
- 내부 구현(클래스 구조/메서드 호출/Mock verify 등)을 “보호 대상”으로 삼지 말 것
- 리팩터링 중 테스트가 깨지는 것을 목표로 하지 말 것  
  → 목표는 **외부 행동이 유지되는 한 테스트는 유지** 되는 것


## Definition: External Behavior (What we protect)

External Behavior 예시:
- HTTP 응답(상태코드/바디/헤더)
- 예외 타입/에러 코드(클라이언트 관점에서 의미 있는 경우)
- DB 상태 변화(주문 생성, 잔액 차감, 재고 감소 등)
- 비즈니스 결과(성공/실패, 처리된 수량, 생성된 리소스)

IMPORTANT:
- `verify(option).decrease(3)` 같은 내부 호출 검증은 보호 대상이 아님
- “선물을 보내면 재고가 감소한다”를 보호할 때는
    - decrease() 호출이 아니라 **재고가 실제로 감소한 결과** 를 검증해야 함


## Working Workflow (Always follow)

1) **프로젝트 핵심 기능 파악**
- 사용자(또는 API consumer) 관점에서 가능한 행동(Use Case) 목록화
- “상태 변화”가 있는 흐름을 우선순위로 선정

2) **행위(behavior) 선정**
- 리팩터링 위험이 큰 구간(트랜잭션/영속성/예외/상태 누락) 우선
- 실패 시 비즈니스 영향이 큰 시나리오 우선

3) **행동 기반 테스트 작성**
- 시스템 경계(API)에서 시나리오를 실행하고
- 최종 상태(결과)로 검증

4) **리팩터링**
- 책임 분리/구조 개선/이름 변경/중복 제거 등 수행
- 테스트는 “외부 행동 유지”를 보장해야 함

5) **문서화**
- 선택 기준/논의 과정/프롬프트를 결과물로 남김


## Test Style Guide (Behavior-driven without BDD tools)

### Test structure
- 각 테스트는 최소한 아래 형태를 갖추기:
    - Given: 초기 상태/준비
    - When: 사용자 행동(주로 API 호출)
    - Then: 외부 행동 결과 검증(상태/응답/부수효과)

### What to assert (priority order)
1) 사용자에게 의미 있는 결과(HTTP 응답, 반환 리소스)
2) 도메인 상태 변화(DB의 핵심 테이블/집계 값)
3) 후속 행동으로 검증 가능한 계약(“다음 행동”이 이전 행동의 결과를 드러내게)

가능하면 “직접 DB 조회” 없이도 검증 가능한 방식(조회 API 등)을 선호.
단, 레거시 상황에서 읽기 전용 검증이 불가피하면 **최소 범위** 로 DB를 조회할 수 있음.

### What NOT to assert
- private method 호출 여부
- 특정 클래스의 존재/구조
- Mock verify 중심의 “내부 구현 계약”
- 세부 로깅 메시지, 내부 이벤트 이름(외부 계약이 아닌 경우)

### Naming
- 테스트 이름은 “행위 + 기대 결과”가 드러나게 작성
    - 예: `should_decrease_inventory_when_sending_gift_successfully`
    - 예: `should_fail_with_out_of_stock_when_quantity_exceeds_inventory`

### Flaky 방지
- 시간 의존 로직은 clock 주입 또는 고정(가능하면)
- 랜덤/동시성은 결정적(deterministic)으로
- 테스트 간 데이터 격리/청소 확실히


## Test Data Strategy (Default rules)

우선순위:
1) **API로 준비(Arrange via API)**: 사용자 관점과 동일한 경로로 데이터 생성
2) 불가할 때만 repository/DAO/SQL로 seed
3) 청소는 각 테스트가 독립적으로 수행

권장 방법(프로젝트에 맞는 것을 선택):
- 트랜잭션 롤백 기반 격리(가능하면)
- 테스트 전/후 테이블 truncate(느리지만 확실)
- Testcontainers 등으로 격리된 DB 사용(가능하면)

IMPORTANT:
- 테스트는 실행 순서에 의존하면 안 됨
- 다른 테스트가 만든 데이터에 기대지 말 것


## Typical “Where system breaks” checklist (Watch-outs)

리팩터링 시 특히 깨지기 쉬운 지점:
- 트랜잭션 경계 변경
- 영속성 컨텍스트 오해
- 도메인 로직 누락(특히 side effect)
- flush/clear 누락으로 인한 상태 불일치
- 예외 타입/에러 코드 변경
- API 계약(응답 스키마) 변경

테스트는 위 위험을 “결과”로 잡아내야 함.


## Commands (How to run)

Claude는 먼저 저장소를 확인해서 실제 명령을 확정해야 함.
아래는 기본 규칙:

### If Gradle wrapper exists
- `./gradlew test`

### If Maven wrapper exists
- `./mvnw test`

### If Node exists (package.json)
- `npm test` 또는 `pnpm test` / `yarn test` (프로젝트 스크립트 기준)

IMPORTANT:
- 새로운 테스트를 추가/수정한 뒤에는 반드시 전체 테스트를 실행
- 실패 재현이 필요한 경우, 실패 케이스를 가장 짧은 경로로 고립시키는 테스트부터 작성


## Project Scanning Checklist (Claude should do this first)

코드를 수정/테스트 작성 전 반드시:
- 진입점 확인: controller/router/handler 경로
- 핵심 도메인(Inventory/Order/Payment/Gift 등) 관련 흐름 추적
- DB 스키마/엔티티/마이그레이션 확인(있다면)
- “상태 변화가 발생하는 행동” 5개 이상 후보 추출
- 현재 실패/버그/불안정 지점이 있다면 우선순위로 테스트화


## Output Conventions (Docs)

### TEST_STRATEGY.md must include
- Behavior list (최소 5개)
    - 각 behavior마다: 사용자 시나리오 / 입력 / 기대 결과 / 실패 시 의미
- Data strategy
    - 준비 방식(API/seed) + 정리 방식(rollback/truncate 등)
- Verification strategy
    - 무엇을(응답/상태/후속행동) 어떻게 검증하는지
- Key decisions
    - 페어 논의에서 갈린 선택지와 
    - 결정 이유

### AI_USAGE.md (or PROMPTS.md)
- 사용한 프롬프트와 결과(요약)
- “프로젝트 핵심 기능 파악”에 쓴 질문들
- 테스트 설계 시 사용한 체크리스트/스킬
- 반복적으로 유효했던 프롬프트 템플릿


## Coding Rules

- 테스트는 읽기 쉽게(과한 추상화 금지)
- 생산 코드 변경은 “행동 기반 테스트가 먼저 보호”된 뒤에 수행
- 리팩터링 중에는 “외부 행동”을 바꾸지 않는다
- 모든 변경은 최소 단위로 커밋/PR 만들기(가능하면)


## Optional imports (keep CLAUDE.md short)
프로젝트에 아래 파일이 있다면 참고:
- See `@README.md` for overall architecture
- See `@package.json` / `@build.gradle` / `@pom.xml` for commands
- See `@docs/` for API contracts (if exists)


