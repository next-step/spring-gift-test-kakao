# PAIR_DECISIONS

## Session 2026-02-20 16:00

### 세션 정보
- 참여자: Human, Claude
- 주제: Claude Code 스킬 구조 검증 · 리팩토링 · 신규 스킬 생성 · 기록 정책 개선
- 관련 문서: TEST_STRATEGY.md, CLAUDE.md, AI_USAGE_LOG.md

### 논의 요약
- `.claude/skills/`와 `.claude/commands/`의 중복 · 역할 차이를 검증
- Skills의 올바른 디렉토리 구조(`skills/이름/SKILL.md`)를 확인하고 리팩토링
- TEST_STRATEGY.md 기반 인수 테스트 작성 스킬 신규 생성
- 스킬 YAML frontmatter 메타데이터 필요성 논의 및 적용
- ai-usage-log-writer의 세션 관리 정책을 "매번 append"에서 "같은 세션이면 Edit"으로 변경

### Decision Log

| 결정 ID | 날짜 | 주제 | 선택한 결정 | 근거 | 고려한 대안 | 영향 범위 | 담당 | 목표일 | 상태(확정/보류) | 근거 링크 |
|---------|------|------|-------------|------|-------------|-----------|------|--------|-----------------|-----------|
| D-001 | 2026-02-20 | skills 폴더 처리 방식 | 삭제 대신 올바른 디렉토리 구조로 리팩토링 | Skills가 Commands의 상위 호환이며 고급 기능(frontmatter, 보조 파일, 자동 트리거) 지원 | skills/ 폴더 삭제 | .claude/skills/ 전체 | Human + Claude | 2026-02-20 | 확정 | Claude Code 공식 문서 |
| D-002 | 2026-02-20 | skills 디렉토리 구조 | `skills/이름/SKILL.md` 디렉토리 구조 채택 | Claude Code가 인식하는 공식 구조. 플랫 파일(`skills/이름.md`)은 인식 불가 | 플랫 파일 유지 + commands만 사용 | 기존 2개 스킬 파일 이동 | Claude | 2026-02-20 | 확정 | Claude Code 문서, 실제 시스템 인식 테스트 |
| D-003 | 2026-02-20 | 인수 테스트 스킬 생성 | TEST_STRATEGY.md + CLAUDE.md + 코드베이스 분석 기반 `acceptance-test-writer` 스킬 생성 | RestAssured 필수 요구사항. 검증 3계층, 데이터 전략, 시나리오 체크리스트를 스킬에 내장 | 스킬 없이 매번 수동 지시 | .claude/skills/acceptance-test-writer/ | Human + Claude | 2026-02-20 | 확정 | TEST_STRATEGY.md, 코드베이스 6개 파일 분석 |
| D-004 | 2026-02-20 | YAML frontmatter 필드 | name, description, argument-hint, allowed-tools 4개 필드 적용 | description이 있어야 Claude 자동 선택 가능. allowed-tools로 권한 제어 | name + description만 최소 적용 | 3개 SKILL.md 전체 | Claude | 2026-02-20 | 확정 | Claude Code 공식 문서, 시스템 인식 확인 |
| D-005 | 2026-02-20 | ai-usage-log-writer 세션 관리 | 같은 세션이면 기존 블록 Edit, 다른 세션이면 append | 같은 대화 내 여러 번 호출 시 중복 블록 생성 방지. 세션 단위 일관성 유지 | 매번 새 블록 append (기존 방식) | ai-usage-log-writer SKILL.md, AI_USAGE_LOG.md | Human + Claude | 2026-02-20 | 확정 | 사용자 피드백: "같은 세션이더라도 새로운 세션을 시작... 원하는 대로 누적되지 않습니다" |
| D-006 | 2026-02-20 | 세션 식별자 방식 | `${CLAUDE_SESSION_ID}` (UUID) 사용 | 타임스탬프는 같은 세션 내 여러 호출 시 미세하게 달라질 수 있음. UUID는 대화 단위로 고유 | 타임스탬프(`YYYY-MM-DD HH:mm`) 기반 | ai-usage-log-writer SKILL.md, AI_USAGE_LOG.md 세션 헤더 | Human | 2026-02-20 | 확정 | 사용자가 직접 SKILL.md 수정하여 ${CLAUDE_SESSION_ID} 적용 |

### Rejected Alternatives

| 대안 | 기각 이유 | 재검토 조건 |
|------|-----------|-------------|
| skills/ 폴더 삭제 | Skills가 Commands 상위 호환이며 고급 기능(frontmatter 등) 활용 가치 있음 | Skills 기능을 전혀 사용하지 않게 될 경우 |
| 플랫 파일(`skills/이름.md`) 유지 | Claude Code가 인식하지 못하는 구조 | Claude Code가 플랫 파일 지원을 추가할 경우 |
| 스킬 없이 매번 수동 지시 | 반복 작업 비효율. 일관성 유지 어려움 | 스킬 유지보수 비용이 수동 지시보다 클 경우 |
| 타임스탬프 기반 세션 관리 | 같은 대화 내 여러 호출 시 시각 차이로 다른 세션으로 인식될 수 있음 | ${CLAUDE_SESSION_ID}를 지원하지 않는 환경에서 사용 시 |

### 다음 액션
- [ ] commands/ 중복 파일 정리 여부 결정 (담당: Human, 기한: 2026-02-21)
- [ ] 이전 세션 로그 헤더 형식 통일 여부 결정 (담당: Human, 기한: 2026-02-21)
- [ ] `/acceptance-test-writer`로 Category API E2E 테스트 작성 시작 (담당: Human + Claude, 기한: 2026-02-21)
- [ ] @RequestBody 누락 버그 수정 (담당: Claude, 기한: 2026-02-21)
