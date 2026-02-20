---
name: smart-commit
description: 수정된 파일들을 분석하고 적절한 커밋 메시지를 생성하여 Git 커밋합니다. 변경이 많으면 논리적 단위로 그룹화하여 여러 커밋을 생성합니다.
disable-model-invocation: true
allowed-tools: Bash(git:*), Read, Grep, Glob
argument-hint: [커밋 관련 추가 지시사항]
---

# Smart Commit

수정된 파일을 분석하고 논리적 단위로 그룹화하여 커밋한다.

## 1단계: 변경 사항 파악

아래 명령어를 병렬로 실행하여 현재 상태를 파악한다.

```bash
git status --short
```

```bash
git diff --stat
```

```bash
git diff --staged --stat
```

```bash
git log --oneline -5
```

- untracked 파일, staged 파일, unstaged 변경 파일을 모두 확인한다.
- 최근 커밋 메시지 스타일을 참고한다.

## 2단계: 변경 내용 분석

변경된 각 파일의 diff를 읽어 **무엇이 왜 변경되었는지** 파악한다.

```bash
git diff <파일>          # unstaged 변경
git diff --staged <파일>  # staged 변경
```

- 새 파일(untracked)은 Read 도구로 내용을 확인한다.
- `.env`, `credentials`, 시크릿이 포함된 파일은 커밋 대상에서 **제외**하고 사용자에게 경고한다.

## 3단계: 그룹화 판단

변경 파일이 **하나의 논리적 변경**에 해당하면 단일 커밋으로 진행한다.

변경이 여러 목적을 가지면 아래 기준으로 그룹화한다:

| 그룹화 기준 | 예시 |
|------------|------|
| 기능 단위 | 새 API 엔드포인트 추가 (컨트롤러 + 서비스 + DTO) |
| 레이어 단위 | 설정 파일 변경, 빌드 스크립트 변경 |
| 문서 단위 | README, 전략 문서 등 |
| 테스트 단위 | 테스트 코드 + 테스트 리소스 |

그룹화 결과를 사용자에게 보여주고 **승인을 받은 후** 커밋을 진행한다.

표시 형식:

```
커밋 1: <커밋 메시지>
  - path/to/file1
  - path/to/file2

커밋 2: <커밋 메시지>
  - path/to/file3
```

## 4단계: 커밋 실행

승인받은 그룹 순서대로 커밋한다. 각 커밋마다:

```bash
git add <파일1> <파일2> ...
```

```bash
git commit -m "$(cat <<'EOF'
<커밋 메시지>

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

## 커밋 메시지 규칙

### 구조

```
타입(스코프): 주제    ← Header (필수)
                      ← 빈 행
본문                  ← Body (선택)
                      ← 빈 행
바닥글                ← Footer (선택)
```

### 7가지 기본 규칙

1. 제목과 본문을 **빈 행으로 구분**한다.
2. 제목은 **50글자 이내**로 제한한다.
3. 제목 첫 글자는 **대문자**로 작성한다.
4. 제목 끝에 **마침표를 붙이지 않는다.**
5. 제목은 **명령문**으로 작성한다. (과거형 금지)
6. 본문 각 행은 **72글자 이내**로 제한한다.
7. 본문에는 **무엇을, 왜** 변경했는지 작성한다. (어떻게 X)

### 타입

| 타입 | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 스타일/포맷 변경 (동작 변경 없음) |
| `refactor` | 코드 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정 등 자잘한 수정 |
| `build` | 빌드 관련 파일/모듈 수정 |
| `ci` | CI 설정 수정 |
| `perf` | 성능 개선 |

### 스코프

변경이 영향을 미치는 범위를 괄호 안에 표기한다. 생략 가능하다.

예: `feat(gift):`, `fix(option):`, `docs(readme):`

### 바닥글

이슈 번호가 있으면 바닥글에 참조한다.

예: `Resolves: #123`, `Related: #456`

### 작성 예시

```
feat(gift): 선물 보내기 API 추가

Member-Id 헤더를 통해 발신자를 식별하고,
옵션 재고를 차감한 뒤 GiftDelivery로 배송을 처리한다.

Resolves: #42
```

### Co-Authored-By

모든 커밋 메시지 마지막에 아래를 포함한다.

```
Co-Authored-By: Claude <noreply@anthropic.com>
```

## 주의사항

- `--force`, `--no-verify`, `--amend`는 사용하지 않는다.
- `git push`는 하지 않는다.
- 커밋 전에 반드시 사용자 승인을 받는다.
- 이미 staged된 파일이 있으면 사용자에게 알리고 처리 방법을 확인한다.

$ARGUMENTS
