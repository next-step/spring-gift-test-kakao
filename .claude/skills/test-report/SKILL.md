---
name: test-report
description: 프로젝트의 모든 테스트를 실행하고 결과를 요약한 PDF 리포트를 생성합니다. 테스트 결과 확인, 리포트 생성, 테스트 요약 요청 시 사용합니다.
disable-model-invocation: true
allowed-tools: Bash(./gradlew:*), Bash(python:*), Bash(python3:*), Bash(pip:*), Bash(pip3:*), Bash(open:*)
---

# 테스트 실행 및 PDF 리포트 생성

아래 단계를 순서대로 수행한다.

## 1단계: 테스트 실행

```bash
cd $PROJECT_DIR && ./gradlew test 2>&1 || true
```

- `|| true`를 붙여 테스트 실패 시에도 다음 단계를 진행한다.
- 테스트 결과 XML은 `build/test-results/test/`에 생성된다.

## 2단계: PDF 리포트 생성

번들 스크립트를 실행하여 테스트 결과 XML을 파싱하고 PDF를 생성한다.

```bash
python3 .claude/skills/test-report/scripts/generate_pdf.py .
```

스크립트가 `fpdf2` 라이브러리를 자동으로 설치한다. 상세 동작은 [scripts/generate_pdf.py](scripts/generate_pdf.py)를 참조한다.

생성되는 PDF 내용:
- 전체 요약 (테스트 수, 성공/실패/에러/스킵, 실행 시간)
- 테스트 클래스별 결과 표
- 실패한 테스트 상세 (테스트명, 에러 메시지)

## 3단계: PDF 열기

```bash
open test-report.pdf
```

## 4단계: 결과 보고

사용자에게 다음을 알려준다:
- 테스트 결과 요약 (전체/성공/실패 수)
- 실패한 테스트가 있으면 실패 내용
- 생성된 PDF 파일 경로
