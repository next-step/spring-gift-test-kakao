# AI_USAGE_LOG

## SessionID: 9e9e9dc2-0bcd-4874-a9d9-c647d81f142a

### 세션 정보
- 목적: Spring Gift Service 프로젝트 문서화 및 E2E 테스트 전략 수립
- 범위: README, CLAUDE.md, TEST_STRATEGY.md 작성 / 커스텀 스킬 생성
- 근거 소스: 대화 로그, 생성된 파일, 코드 분석 결과

### 프롬프트 및 결과

| ID | 목적 | 프롬프트 원문(마스킹) | 기대 결과 | 실제 결과 | 판단 | 이유 | 근거 |
|----|------|------------------------|-----------|-----------|------|------|------|
| A1 | 로그인 기능 탐색 | `login` | 불명확 | 코드베이스 탐색 → 인증 미구현 확인 → 로그인 유형 질문 → 사용자 "구현하지마" 응답 | 기각 | 사용자 의도와 불일치. 단일 단어 프롬프트는 의도 파악 어려움 | 대화 로그 |
| A2 | README 작성 | `현재 프로젝트의 주요 기능 사항을 리드미로 작성해줘` | README.md 생성 | README.md 작성 완료 (기술스택, 기능, API, 구조, 실행방법) | 채택 | 재현성: 코드 분석 기반으로 정확한 문서 생성. 유지보수성: 프로젝트 변경 시 수정 용이 | README.md 파일 |
| A3 | README에 ERD 추가 | AskUserQuestion으로 추가 항목 질문 → 사용자 `ERD/도메인 관계 추가` 선택 | 도메인 관계 다이어그램 추가 | ASCII 다이어그램 + 엔티티 설명 테이블 README에 추가 | 채택 | 검증력: 엔티티 관계를 시각적으로 표현하여 이해도 향상 | README.md 파일 |
| A4 | 도메인 관계 검증 | `Product에 Option이 있는게 맞아?` | 코드 기반 사실 확인 | Option→Product 단방향 ManyToOne 관계 확인. DB 관계상 1:N은 맞지만 Product에서 getOptions() 불가 | 채택 | 검증력: 실제 코드(Option.java:17-18) 확인으로 정확성 보장 | Option.java `@ManyToOne private Product product` |
| A5 | CLAUDE.md 작성 | `테스트 리드 엔지니어로서 CLAUDE.md 작성` + 7개 섹션 요구사항 명세 | E2E 테스트 가이드 문서 | CLAUDE.md 7개 섹션 작성 완료. 코드 이슈 4건 발견 및 문서화 | 채택 | 검증력: 실제 코드 분석 기반 시나리오 매트릭스. 재현성: 구체적 TODO와 실행 계획 포함 | CLAUDE.md 파일 |
| A6 | RestAssured 명시 | `E2E 테스트 진행시 RestAssured 사용하도록 명시` | CLAUDE.md 테스트 스택 수정 | MockMvc → RestAssured 변경 (5곳 수정: 기술스택, 설정 예시, 데이터 격리, TODO, 헬퍼) | 채택 | 유지보수성: RestAssured는 실제 HTTP 스택 테스트로 E2E 목적에 부합. @Transactional 롤백 불가 주의사항도 반영 | CLAUDE.md 파일 |
| A7 | TEST_STRATEGY.md 작성 | `테스트 전략 문서 작성` (검증 행위, 데이터 전략, 검증 전략, 의사결정 4개 섹션) | 테스트 전략 문서 | TEST_STRATEGY.md 작성 완료. P0~P3 우선순위 체계, 3계층 검증 전략, 6개 의사결정 기록 | 채택 | 검증력: API별 검증 포인트 코드 예시 포함. 재현성: 의사결정 근거와 트레이드오프 명시 | TEST_STRATEGY.md 파일 |
| A8 | 커스텀 스킬 생성 | `ai-usage-log-writer, pair-decision-recorder 스킬 생성` | 2개 스킬 파일 | .claude/skills/ 에 2개 .md 파일 생성 | 채택 | 유지보수성: 반복 작업을 스킬로 구조화 | .claude/skills/*.md |
| A9 | 스킬 트리거 문제 해결 | `/ai-usage-log-writer 으로 스킬이 트리거 되지 않습니다` | 슬래시 명령어 동작 | .claude/commands/ 디렉토리에 복사하여 해결 | 채택 | 재현성: Claude Code는 .claude/commands/*.md를 슬래시 명령어로 인식 | .claude/commands/*.md |

### 접근법 요약

| 접근법 | 적용 범위 | 결과 | 유지 여부 | 메모 |
|--------|-----------|------|-----------|------|
| Task(Explore) 에이전트로 코드베이스 탐색 | A1, A2, A5 | 성공 | 유지 | 전체 구조 파악에 효과적. 단, 느리므로 단순 파일 확인은 직접 Read 사용 |
| AskUserQuestion으로 요구사항 명확화 | A1, A3 | 성공 | 유지 | 단일 단어 프롬프트(A1)나 추가 옵션(A3) 확인에 유용 |
| 코드 직접 읽기(Read)로 사실 검증 | A4, A6 | 성공 | 유지 | 엔티티 관계, 컨트롤러 어노테이션 등 정확한 확인 필요 시 |
| Edit으로 기존 문서 점진적 수정 | A6 | 성공 | 유지 | CLAUDE.md 5곳 수정 시 전체 재작성 대신 부분 수정으로 안전하게 변경 |

### 생성/수정된 산출물

| 파일 | 목적 | 상태 |
|------|------|------|
| README.md | 프로젝트 개요 문서 | 완료 |
| CLAUDE.md | E2E 테스트 가이드 (RestAssured 기반) | 완료 |
| TEST_STRATEGY.md | 테스트 전략 문서 | 완료 |
| .claude/skills/ai-usage-log-writer.md | AI 활용 로그 스킬 정의 | 완료 |
| .claude/skills/pair-decision-recorder.md | 페어 결정 기록 스킬 정의 | 완료 |
| .claude/commands/ai-usage-log-writer.md | 슬래시 명령어 등록 | 완료 |
| .claude/commands/pair-decision-recorder.md | 슬래시 명령어 등록 | 완료 |

### 최종 가이드

- 재사용 프롬프트:
  - `현재 프로젝트의 주요 기능 사항을 리드미로 작성해줘` → 코드 분석 기반 README 생성
  - `테스트 리드 엔지니어로서 CLAUDE.md 작성` + 섹션 요구사항 → 실행 가능한 테스트 가이드
  - `테스트 전략 문서 작성` + 4개 섹션 명시 → 의사결정 포함 전략 문서
- 주의점:
  - 단일 단어 프롬프트(`login`)는 의도 파악 실패 → 목적과 범위를 함께 전달할 것
  - .claude/skills/ 만으로는 슬래시 명령어 미동작 → .claude/commands/에도 배치 필요
  - MockMvc vs RestAssured 선택은 데이터 격리 방식에 영향 → 초기에 명시적으로 결정할 것
---

## SessionID: 470361d1-9ba3-41dd-9e95-6b219c6e8926

### 세션 정보
- 목적: Claude Code skills/commands 구조 검증, 리팩토링, 신규 스킬 생성, 메타데이터 추가, 스킬 기록 정책 개선
- 범위: .claude/skills/, .claude/commands/ 디렉토리 구조 전체
- 근거 소스: 대화 로그, 파일 비교, Claude Code 공식 문서 조사, 파일 변경 내역

### 프롬프트 및 결과

| ID | 목적 | 프롬프트 원문(마스킹) | 기대 결과 | 실제 결과 | 판단(채택/기각/보류) | 이유(재현성/유지보수성/검증력/비용) | 근거 |
|----|------|------------------------|-----------|-----------|----------------------|--------------------------------------|------|
| B1 | skills 폴더 삭제 가능 여부 검증 | `.claude 하위에 skills 폴더와 commands 폴더가 있는데 skills 폴더를 삭제해도 무방한지 검증해주세요` | 삭제 가능 여부 판단 | skills/와 commands/ 파일 내용 100% 동일 확인. commands/가 실제 슬래시 커맨드 로딩 경로 → 삭제 가능 판단 | 채택 | 검증력: 4개 파일 병렬 읽기로 내용 동일성 확인 | 4개 파일 내용 비교 결과 |
| B2 | skills vs commands 관계 조사 | `skills는 왜 anthropic에서 만들었는지 알려주세요. commands가 최신에 나오고 skills가 deprecated 된 것인가요?` | 두 기능의 역사와 관계 | Skills가 Commands의 상위 호환(superset). Commands 먼저 → Skills 나중. deprecated 아님, 공존 | 채택 | 재현성: Claude Code 공식 문서 기반 조사 | Task(claude-code-guide) 에이전트 조사 결과 |
| B3 | skills 미인식 원인 분석 | `skills 폴더에 .md 파일들이 있었는데, 이때는 왜 슬래시 커맨드 생성이 안되었는지 설명해줘` | 구조적 원인 설명 | Skills는 `skills/이름/SKILL.md` 디렉토리 구조 필요. 플랫 파일(`skills/이름.md`)은 인식 불가 | 채택 | 검증력: Commands(플랫) vs Skills(디렉토리) 구조 차이 명확화 | Claude Code 문서 |
| B4 | skills 디렉토리 리팩토링 | `skills/이름/SKILL.md 이렇게 리팩토링해줘` | 플랫 파일 → 디렉토리 구조 변환 | `skills/ai-usage-log-writer/SKILL.md`, `skills/pair-decision-recorder/SKILL.md`로 변환 완료 | 채택 | 재현성: mkdir + mv로 안전한 변환. 시스템에서 스킬 인식 확인 | 파일 구조 변경 + 시스템 인식 확인 |
| B5 | 인수 테스트 작성 스킬 생성 | `@TEST_STRATEGY.md 해당 파일을 기반으로 인수테스트를 잘 작성하는 skill을 만들어줘. 이때 필수적으로, RestAssured를 기반으로 테스트 코드를 작성할 수 있어야해` | RestAssured 기반 E2E 스킬 생성 | `acceptance-test-writer/SKILL.md` 생성 완료. 테스트 클래스 구조, 검증 3계층, 데이터 전략, API별 시나리오, 실행 흐름 포함 | 채택 | 검증력: TEST_STRATEGY.md + CLAUDE.md + 코드베이스 6개 파일 분석 기반. 재현성 높음 | 스킬 파일 생성 확인 |
| B6 | 스킬 YAML frontmatter 추가 | `현재 .claude/**/SKILL.md에 상단에 메타 데이터가 없습니다. 적어도 name, description은 필요할 것 같으며 agents가 skill을 잘 사용할 수 있도록 메타 데이터를 추가해주세요` | 3개 스킬에 frontmatter 추가 | name, description, argument-hint, allowed-tools 4개 필드를 3개 스킬 모두에 추가 | 채택 | 유지보수성: Claude 자동 선택 가능. 비용: 즉시 시스템 인식 확인 | 시스템 리마인더에서 3개 스킬 description 노출 확인 |
| B7 | 스킬 기록 정책 개선 | `같은 세션일 때 이미 정리된 템플릿이 있으면 해당 템플릿을 수정하는 방식을 원합니다` | 같은 세션 = Edit, 다른 세션 = append | 파일 기록 정책에 세션 판별 규칙 추가. ${CLAUDE_SESSION_ID} 기반 블록 매칭 → Edit 또는 append 분기 | 채택 | 유지보수성: 세션 단위 로그 관리 일관성. 재현성: 세션 ID 기반 자동 판별 | SKILL.md 파일 변경 확인 |

### 접근법 요약

| 접근법 | 적용 범위 | 결과 | 유지 여부 | 메모 |
|--------|-----------|------|-----------|------|
| 병렬 파일 읽기 + 내용 비교 | B1: skills/ vs commands/ 동일성 검증 | 성공 | 유지 | 4개 파일 동시 읽기로 빠른 비교 |
| Task(claude-code-guide) 에이전트 | B2, B6: 공식 문서 조사 | 성공 | 유지 | 전문 에이전트로 정확한 정보 획득 |
| mv 명령으로 구조 변환 | B4: skills 디렉토리 리팩토링 | 성공 | 유지 | mkdir + mv로 안전한 파일 이동 |
| 코드베이스 분석 → 스킬 생성 | B5: acceptance-test-writer | 성공 | 유지 | 컨트롤러/서비스/엔티티 6개 파일 분석으로 구체적 패턴 추출 |
| ${CLAUDE_SESSION_ID} 기반 세션 판별 | B7: 기록 정책 개선 | 성공 | 유지 | 같은 세션 Edit vs 다른 세션 append 분기 |

### 생성/수정된 산출물

| 파일 | 변경 내용 | 상태 |
|------|-----------|------|
| .claude/skills/ai-usage-log-writer/SKILL.md | 플랫→디렉토리 구조 변환 + frontmatter 추가 + 세션 판별 규칙 추가 | 완료 |
| .claude/skills/pair-decision-recorder/SKILL.md | 플랫→디렉토리 구조 변환 + frontmatter 추가 | 완료 |
| .claude/skills/acceptance-test-writer/SKILL.md | 신규 생성 + frontmatter 추가 | 완료 |

### 발견 사항

| 항목 | 내용 |
|------|------|
| Skills 구조 규칙 | `.claude/skills/이름/SKILL.md` (디렉토리 구조 필수). 플랫 파일은 인식 불가 |
| Commands 구조 규칙 | `.claude/commands/이름.md` (플랫 파일) |
| 동일 이름 우선순위 | Skills가 Commands보다 우선 |
| Skills 고유 기능 | YAML frontmatter(name, description, argument-hint, allowed-tools 등), 보조 파일, 자동 트리거, 도구 제어 |
| frontmatter 핵심 필드 | `description`이 있어야 Claude 자동 선택 가능 |
| 세션 관리 | `${CLAUDE_SESSION_ID}`로 같은 대화 내 호출 구분 가능 |

### 최종 가이드

- 재사용 프롬프트:
  - `@파일명 해당 파일을 기반으로 [목적] skill을 만들어줘` — 참조 문서 기반 스킬 생성
  - `SKILL.md에 메타 데이터를 추가해주세요` — frontmatter 일괄 추가
  - `skills/이름/SKILL.md 이렇게 리팩토링해줘` — 구조 변환

- 주의점:
  - Skills는 반드시 디렉토리 구조(`skills/이름/SKILL.md`) — 플랫 파일은 인식 안 됨
  - frontmatter의 `description`이 없으면 Claude 자동 선택 불가
  - commands/와 skills/에 같은 이름이 있으면 skills가 우선

---

## SessionID: bbf7d576-831f-4b3a-b249-07e635319651

### 세션 정보
- 목적: Category/Product/Gift API E2E 인수 테스트 작성, 프로덕션 버그 문서화, logical-commit 스킬 생성
- 범위: CategoryAcceptanceTest.java, ProductAcceptanceTest.java, GiftAcceptanceTest.java, acceptance-test-writer 스킬, logical-commit 스킬
- 근거 소스: 대화 로그, 테스트 실행 결과(`./gradlew test`), git 커밋 이력

### 프롬프트 및 결과

| ID | 목적 | 프롬프트 원문(마스킹) | 기대 결과 | 실제 결과 | 판단(채택/기각/보류) | 이유(재현성/유지보수성/검증력/비용) | 근거 |
|----|------|------------------------|-----------|-----------|----------------------|--------------------------------------|------|
| C1 | POST /api/categories 테스트 계획 수립 | Plan 모드에서 CategoryRestController 분석 → POST 테스트 계획 작성 | 프로덕션 버그 고려한 테스트 계획 | @Disabled 기대 동작 + 현재 동작 확인 테스트 2개 계획. 프로덕션 수정 없이 테스트만 작성하는 전략 수립 | 채택 | 검증력: 프로덕션 버그를 테스트로 문서화. 유지보수성: 수정 후 @Disabled 해제만으로 전환 가능 | Plan 파일 |
| C2 | POST 테스트 구현 | Plan 승인 후 CategoryAcceptanceTest.java 수정 | 테스트 3개 통과 + 1개 Disabled | `./gradlew test` BUILD SUCCESSFUL. GET 2개 + POST 현재 동작 1개 통과, POST 기대 동작 1개 Disabled | 채택 | 검증력: 프로덕션의 실제 동작(200 + name=null) 검증. 재현성: 테스트 전체 통과 확인 | 테스트 실행 결과 |
| C3 | 주석 인과관계 수정 | 사용자: `@RequestBody는 Request 관련 어노테이션인데 주석이 Response를 설명하고 있어서 혼란` | 인과 체인 명확화 | `@RequestBody 누락 → JSON 역직렬화 안 됨 → name=null로 저장됨`으로 수정. 요청→저장→응답 전파 과정 표현 | 채택 | 유지보수성: 주석만 보고 버그의 원인-결과를 이해할 수 있음 | CategoryAcceptanceTest.java 106행 |
| C4 | 논리적 단위 커밋 | `현재 작업한 사항을 논리적 작업 단위에 맞춰 commit 해주세요` | 관련 파일끼리 묶인 커밋 | 3개 커밋 생성: (1) test: RestAssured 의존성+H2 설정 (2) test: Category API E2E 테스트 (3) chore: 스킬 규칙 강화 | 채택 | 유지보수성: 인프라→테스트→설정 순서로 의존 관계 반영. 재현성: 각 커밋이 독립적으로 의미 있음 | git log d8e5384, dc1ba56, 2cb454a |
| C5 | logical-commit 스킬 생성 | `변경 사항을 논리적 작업 단위에 맞춰 commit 할 수 있도록 하는 skill 만들어줘` | 커밋 자동 분류 스킬 | `.claude/skills/logical-commit/SKILL.md` 생성. 5단계 절차(수집→분류→순서→확인→실행), 그룹핑 기준, 커밋 메시지 규칙 포함 | 채택 | 재현성: 반복 커밋 작업을 `/logical-commit`으로 표준화. 유지보수성: 사용자 확인 단계 필수로 안전성 확보 | 스킬 파일 생성 확인 |
| C6 | GET /api/products 테스트 작성 | `/acceptance-test-writer GET /api/products` | Product 목록 조회 테스트 | ProductAcceptanceTest.java 생성. 빈 목록(200+[]) + N개 존재(200+구체값 검증) 2개 테스트 통과 | 채택 | 검증력: Repository로 데이터 준비 후 GET 검증. 재현성: FK 역순 삭제로 테스트 격리 | 테스트 실행 결과, git 99b04c8 |
| C7 | notNullValue → 구체값 검증 개선 | 사용자: `not null value는 무책임해. 구체적으로 검증하고 [1]도 검증해야대` | 구체적 값 검증 + [1] 추가 | notNullValue() → equalTo("노트북"), equalTo(1_500_000) 등으로 변경. [1] 키보드 검증 추가 | 채택 | 검증력: 구체값 검증이 notNullValue보다 회귀 방지 효과 높음. 비용: 변경 최소 | ProductAcceptanceTest.java |
| C8 | POST /api/products 테스트 작성 | `/acceptance-test-writer POST /api/products` | POST 성공/실패 테스트 | 3개 테스트 추가: 현재 동작(500) 1개 활성 + @Disabled 2개(성공, 존재하지 않는 카테고리). Category와 달리 categoryId=null → findById(null) → 500 | 채택 | 검증력: Category 버그와 다른 실패 경로(500 vs 200+null) 문서화. 유지보수성: @Disabled로 기대 동작 보존 | 테스트 실행 결과 |
| C9 | POST /api/gifts 테스트 작성 | `/acceptance-test-writer POST /api/gifts` | Gift 전송 성공/실패 테스트 | 5개 테스트 작성(모두 활성): 성공(200+재고 감소), 헤더 누락(400), 옵션 미존재(500), 재고 부족(500+롤백), 발신자 미존재(500+롤백) | 채택 | 검증력: Layer 3(DB 상태 변화) 검증 포함. 트랜잭션 롤백까지 확인 | 테스트 실행 결과, git 8300e41 |
| C10 | 테스트 간 FK 충돌 해결 | GiftAcceptanceTest 추가 후 ProductAcceptanceTest 실패 → setUp 분석 | 모든 테스트 통과 | Category/Product 테스트의 setUp에 optionRepository.deleteAll() 추가. 테스트 클래스 간 DB 공유로 인한 FK 제약 충돌 해결 | 채택 | 재현성: 모든 테스트 클래스에서 전체 FK 역순 삭제로 격리 보장. 비용: import + 3행 추가 | git d679878, ./gradlew test 전체 통과 |
| C11 | Category/Product POST 후 DB 저장 검증 | 사용자: `DB로 조회해서 실제로 데이터가 저장 되어있는지 검증` | Layer 3 DB 검증 테스트 | Category: 1건 저장 + name=null 확인. Product: 500 에러로 저장 안 됨(isEmpty) 확인. 같은 @RequestBody 버그지만 DB 결과가 다름 | 채택 | 검증력: HTTP 응답만으로는 DB 저장 여부 확신 불가. Layer 3 검증으로 실제 상태 확인 | git 23731c6, ./gradlew test 전체 통과 |

### 접근법 요약

| 접근법 | 적용 범위 | 결과 | 유지 여부 | 메모 |
|--------|-----------|------|-----------|------|
| @Disabled로 기대 동작 보존 + 현재 동작 테스트 분리 | C1, C2: POST 테스트 | 성공 | 유지 | 프로덕션 수정 불가 시 두 테스트를 병행하여 버그와 기대 동작 모두 문서화 |
| 사용자 피드백으로 주석 인과관계 개선 | C3: 주석 수정 | 성공 | 유지 | 요청→저장→응답 전파 체인을 명시하면 혼란 방지 |
| 의존 순서 기반 커밋 분리 | C4: 논리적 커밋 | 성공 | 유지 | 인프라→코드→설정 순서로 커밋하면 bisect/revert 시 유리 |
| Repository로 테스트 데이터 준비 | C6, C8: Product 테스트 | 성공 | 유지 | POST API에 @RequestBody 버그 있어 API 호출 불가 → Repository 직접 사용 |
| 구체값 검증 (equalTo > notNullValue) | C7: 검증 개선 | 성공 | 유지 | notNullValue는 회귀 방지에 무의미. 실제 기대값으로 검증해야 함 |
| 전체 FK 역순 삭제로 테스트 격리 | C10: setUp 보강 | 성공 | 유지 | 테스트 클래스 간 DB 공유 시, 자기가 안 쓰는 테이블도 FK 역순으로 삭제해야 함 |
| Layer 3 DB 상태 검증 (assertThat) | C9: Gift 테스트 | 성공 | 유지 | 상태 변경 API는 HTTP 응답만으로 부족. DB에서 재고 변화/롤백을 직접 확인 |

### 생성/수정된 산출물

| 파일 | 변경 내용 | 상태 |
|------|-----------|------|
| src/test/java/gift/CategoryAcceptanceTest.java | POST 테스트 2개 추가 (@Disabled 기대 동작 + 현재 동작 확인), 버그 주석 | 완료 |
| .claude/skills/acceptance-test-writer/SKILL.md | 프로덕션 수정 금지 규칙, 테스트 실패 대응 절차, 파일 수정 범위 제약 추가 | 완료 |
| .claude/skills/logical-commit/SKILL.md | 신규 생성. 변경 사항 분석→그룹핑→커밋 자동화 스킬 | 완료 |
| src/test/java/gift/ProductAcceptanceTest.java | 신규 생성. GET 2개(빈 목록, N개 구체값 검증) + POST 3개(현재 동작 500, @Disabled 성공, @Disabled 카테고리 미존재) | 완료 |
| src/test/java/gift/GiftAcceptanceTest.java | 신규 생성. 성공(200+재고 감소) + 실패 4개(헤더 누락 400, 옵션 미존재 500, 재고 부족 500, 발신자 미존재 500) | 완료 |

### 발견 사항

| 항목 | 내용 |
|------|------|
| @RequestBody 누락 영향 | JSON body 역직렬화 안 됨 → 모든 필드 null → DB에 null 저장 → 응답에도 null 전파 |
| @ResponseStatus 미설정 | Spring 기본값 200 반환. 201 CREATED 필요 시 명시적 어노테이션 필요 |
| 테스트로 버그 문서화 | @Disabled + 현재 동작 테스트 병행이 프로덕션 수정 없이 버그를 기록하는 효과적 패턴 |
| 주석 작성 원칙 | 응답 검증 주석은 "왜 이 값인가"의 전체 인과 체인(요청→처리→저장→응답)을 포함해야 함 |
| @RequestBody 누락의 엔티티별 차이 | Category: name=null로 저장 → 200 반환. Product: categoryId=null → findById(null) → IllegalArgumentException → 500 반환. 같은 버그라도 엔티티 구조에 따라 실패 양상이 다름 |
| notNullValue 검증은 불충분 | 값이 null이 아닌 것만 확인하면 잘못된 값이 들어와도 통과함 → 구체적인 equalTo 사용 필수 |
| 테스트 클래스 간 DB 공유 문제 | 같은 Spring Context를 공유하면 다른 테스트 클래스의 데이터가 남아 FK 제약 충돌 발생. 모든 테스트 클래스에서 전체 테이블 FK 역순 삭제 필요 |
| GiftRestController는 @RequestBody 정상 | Category/Product와 달리 Gift API는 @RequestBody 있음. 프로덕션 버그 없이 정상 동작하여 5개 테스트 모두 활성 상태 |
| @Transactional 롤백 검증 | 재고 부족/발신자 미존재 시 예외 발생 → @Transactional에 의해 option.decrease()도 롤백됨. DB에서 재고 미변경 확인으로 검증 |
| 같은 버그, 다른 DB 결과 | Category POST: 200 + name=null로 저장됨. Product POST: 500 + 저장 안 됨(트랜잭션 롤백). 응답 코드뿐 아니라 DB 상태도 검증해야 차이 확인 가능 |

### 최종 가이드

- 재사용 프롬프트:
  - `/acceptance-test-writer <METHOD> <endpoint>` — API별 E2E 테스트 점진적 작성
  - `/logical-commit` — 변경 사항을 논리적 단위로 자동 분류 후 커밋

- 주의점:
  - 프로덕션 버그 주석은 요청→응답 인과 체인 전체를 기술할 것 (한쪽만 언급하면 혼란)
  - `notNullValue()` 대신 `equalTo(구체값)` 사용 — 사용자 피드백: "무책임한 검증"
  - 같은 @RequestBody 누락이라도 엔티티별 실패 양상이 다름 → 각각 현재 동작 테스트 필요
