---
name: organize-docs
description: 프로젝트 루트에 흩어진 마크다운(.md) 파일을 docs/ 폴더로 분류·정리합니다. "문서 정리", "md 파일 정리", "마크다운 정리" 요청 시 사용합니다.
disable-model-invocation: true
allowed-tools: Bash(mv:*), Bash(mkdir:*), Bash(ls:*), Glob, Read, Grep
argument-hint: [정리 대상 경로 (기본: 프로젝트 루트)]
---

# 마크다운 파일 정리

프로젝트 루트에 흩어진 `.md` 파일을 `docs/` 폴더 아래 카테고리별로 분류·이동한다.

## 이동하지 않는 파일 (고정 파일)

아래 파일은 프로젝트 루트에 그대로 둔다:

- `README.md` — 프로젝트 대문
- `CLAUDE.md` — Claude Code 설정
- `CHANGELOG.md` — 변경 이력
- `LICENSE.md` — 라이선스
- `CONTRIBUTING.md` — 기여 가이드

## 실행 절차

### 1단계: 현황 파악

프로젝트 루트의 `.md` 파일 목록을 수집한다.

```bash
ls -1 *.md 2>/dev/null
```

- 고정 파일을 제외한 이동 대상 파일을 식별한다.
- 이동 대상이 없으면 "정리할 파일이 없습니다"를 보고하고 종료한다.

### 2단계: 파일 내용 분석 및 분류

각 이동 대상 파일의 내용을 Read 도구로 읽고, 아래 카테고리로 분류한다.

| 카테고리 | 폴더명 | 기준 |
|----------|--------|------|
| 리뷰 | `docs/review/` | 코드 리뷰, 테스트 리뷰, 리뷰 피드백 등 |
| 전략/설계 | `docs/strategy/` | 테스트 전략, 설계 문서, 아키텍처 결정 등 |
| 요구사항 | `docs/requirements/` | STEP, 미션, 과제, 요구사항 명세 등 |
| 프롬프트 | `docs/prompts/` | 프롬프트, 템플릿, 지시문 등 |
| 기타 | `docs/misc/` | 위 카테고리에 해당하지 않는 문서 |

- 파일명과 내용(첫 50줄)을 기준으로 판단한다.
- 하나의 파일이 여러 카테고리에 해당하면 **가장 핵심적인** 카테고리 하나만 선택한다.

### 3단계: 분류 결과 확인

분류 결과를 아래 형식으로 사용자에게 보여주고 **승인을 받은 후** 이동한다.

```
📁 docs/review/
  - CODE_REVIEW.md
  - TEST_REVIEW.md
  - TEST_REVIEW2.md

📁 docs/strategy/
  - TEST_STRATEGY.md

📁 docs/requirements/
  - STEP1.md

📁 docs/prompts/
  - PROMPT.md

⏭️ 이동하지 않는 파일:
  - README.md (프로젝트 대문)
  - CLAUDE.md (Claude 설정)
```

### 4단계: 폴더 생성 및 파일 이동

승인을 받으면 필요한 폴더를 만들고 파일을 이동한다.

```bash
mkdir -p docs/<카테고리>
mv <파일명> docs/<카테고리>/
```

### 5단계: 결과 보고

이동 결과를 요약하여 보고한다.

- 이동한 파일 수
- 카테고리별 파일 목록
- 최종 `docs/` 폴더 구조

## 주의사항

- 파일을 **복사가 아닌 이동**(mv)한다.
- 이미 `docs/` 폴더에 같은 이름의 파일이 있으면 사용자에게 알리고 처리 방법을 확인한다.
- Git 추적 중인 파일은 `git mv`를 사용하지 않는다 (이동 후 사용자가 직접 커밋).
- `$ARGUMENTS`가 주어지면 해당 경로를 대상으로 실행한다.

$ARGUMENTS
