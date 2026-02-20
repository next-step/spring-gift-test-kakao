---
name: logical-commit
description: >
  변경 사항을 논리적 작업 단위로 분류하여 각각 별도의 커밋으로 생성한다.
  git status/diff를 분석하고, 관련 파일을 그룹핑하여 의미 있는 커밋 메시지와 함께 커밋한다.
argument-hint: "[커밋 범위 힌트, e.g. 테스트 추가분만]"
allowed-tools: Bash, Read, Grep, Glob
---

# logical-commit

변경 사항을 분석하여 논리적 작업 단위별로 커밋을 생성한다.

## 역할
- 현재 변경 사항(staged + unstaged + untracked)을 분석한다.
- 관련 파일을 논리적 단위로 그룹핑한다.
- 각 그룹을 의존 순서에 맞게 별도 커밋으로 생성한다.

## 실행 절차

### 1단계: 변경 사항 수집
다음 명령어를 **병렬로** 실행하여 현재 상태를 파악한다:
- `git status` — 변경/추가/삭제된 파일 목록
- `git diff` — unstaged 변경 내용
- `git diff --cached` — staged 변경 내용
- `git log --oneline -5` — 최근 커밋 메시지 스타일 확인
- `git ls-files --others --exclude-standard` — untracked 파일 목록

untracked 파일은 파일명만 보고 커밋하지 말고, 내용 검토를 먼저 수행한다:
- 텍스트 파일: 앞부분(예: 200줄) 확인 후 커밋 대상 여부 판단
- 바이너리/대용량 파일: 필요성과 출처를 사용자에게 확인

### 2단계: 논리적 그룹 분류
변경된 파일들을 아래 기준으로 그룹핑한다:

**그룹핑 기준 (우선순위 순):**
1. **기능 단위**: 같은 기능을 구성하는 파일끼리 묶음
   - 예: Controller + Service + Entity 변경이 하나의 기능이면 한 커밋
2. **계층 단위**: 인프라/설정 → 프로덕션 코드 → 테스트 코드
   - 예: `build.gradle` 의존성 추가는 별도 커밋
3. **목적 단위**: 버그 수정, 기능 추가, 리팩토링, 문서, 설정은 각각 분리
   - 예: 버그 수정과 새 기능 추가를 한 커밋에 섞지 않음

**분류 예시:**
| 변경 파일 | 그룹 | 커밋 타입 |
|-----------|------|-----------|
| `build.gradle` (의존성 추가) | 인프라 설정 | `chore:` |
| `src/test/resources/application.properties` | 인프라 설정 | 위와 같은 커밋 |
| `src/test/java/.../SomeTest.java` | 테스트 추가 | `test:` |
| `src/main/java/.../Controller.java` | 버그 수정 | `fix:` |
| `.claude/skills/...` | 도구 설정 | `chore:` |
| `*.md` (문서) | 문서 | `docs:` |

### 3단계: 커밋 순서 결정
의존성을 고려하여 커밋 순서를 정한다:
1. 인프라/설정 변경 (다른 코드가 의존하는 기반)
2. 프로덕션 코드 변경
3. 테스트 코드
4. 문서/도구 설정

### 4단계: 사용자 확인
커밋 실행 전에 분류 결과를 사용자에게 보여주고 확인을 받는다:

```
커밋 계획:
1. [chore] build.gradle, src/test/resources/application.properties — "RestAssured 의존성 및 테스트 설정 추가"
2. [test] CategoryAcceptanceTest.java — "Category API E2E 인수 테스트 추가"
3. [chore] SKILL.md — "스킬 규칙 강화"

진행할까요?
```

### 5단계: 커밋 실행
승인 후 각 그룹을 순서대로 커밋한다:
- `git add <파일들>` — 해당 그룹의 파일만 staging
- `git commit -m "<type>: <요약>"` — 비대화형으로 실행
- 본문이 필요하면 `git commit -m "<type>: <요약>" -m "<본문>"` 사용
- 각 커밋 후 성공 여부 확인

커밋 실패 시 즉시 중단한다:
1. `git status --short`로 상태 재확인
2. 실패 원인과 현재 staged 파일을 사용자에게 공유
3. 사용자 확인 전 다음 커밋으로 진행하지 않음

## 커밋 메시지 규칙

### 타입 프리픽스
| 프리픽스 | 용도 |
|----------|------|
| `feat:` | 새 기능 추가 |
| `fix:` | 버그 수정 |
| `test:` | 테스트 추가/수정 |
| `refactor:` | 동작 변경 없는 코드 개선 |
| `chore:` | 빌드, 설정, 도구 변경 |
| `docs:` | 문서 추가/수정 |

### 메시지 형식
```
<type>: <한글 요약 (50자 이내)>

<본문: 변경 이유와 주요 내용 (선택)>

Co-Authored-By: <모델명> <noreply@example.com>  # 선택, 팀 정책 허용 시에만
```

### 메시지 작성 기준
- 제목은 **무엇을 했는지** 명확하게
- 본문은 **왜 했는지** 필요한 경우에만
- 이 저장소의 최근 커밋 스타일을 따른다

## 금지 사항
- 사용자 확인 없이 커밋 실행
- `git add -A` 또는 `git add .` 사용 (파일을 명시적으로 지정)
- 민감한 파일 커밋:
  - `.env`, `.env.*`
  - `application*.properties`, `application*.yml`의 실제 비밀값 포함 파일
  - `*.pem`, `*.key`, `id_rsa`, `id_ed25519`, `*.p12`, `*.jks`
  - 토큰/비밀번호/접속문자열이 포함된 임시 메모 파일
- `--amend`, `--force` 등 이력 변경 옵션 사용
- `--no-verify`로 훅 우회

## 사용 예시

```text
/logical-commit
```

```text
/logical-commit 테스트 추가분만
```
