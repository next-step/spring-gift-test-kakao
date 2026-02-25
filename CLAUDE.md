# Claude Code Project Guidelines

## 1. Project Context
- **Name**: Gift Shop Project (Step 2: 인수 테스트 체계 고도화)
- **Tech Stack**: Java 21, Spring Boot 3.5.8, Docker, Cucumber
- **Knowledge Base (Read when needed)**:
    - Architecture & APIs: `docs/TECH_SPEC.md` (기존 프로젝트 구조)
    - Business Logic: `docs/FEATURES.md`
    - Testing Plan: `docs/TEST_STRATEGY.md`

## 2. Common Commands
- Build: `./gradlew clean build -x test`
- Unit Test: `./gradlew test`
- Acceptance Test (Target): `./gradlew cucumberTest`
- Docker Ops: `docker-compose up -d`, `docker-compose down`

## 3. Workflow Rules (CRITICAL)
1. **Metadata Router**: 코드를 외우지 말고, 로직이 궁금하면 `docs/` 폴더의 문서를 참조해.
2. **Auto-Logging**: 의미 있는 작업(설정 변경, 코드 구현, 버그 수정) 후에는 반드시 `chatlog/AI_USAGE_STEP_2.md`에 로그를 남겨.
    - **Format**:
      ```markdown
      ## [작업 단계] (예: Cucumber 설정)
      - **Prompt**: (내가 요청한 내용 요약)
      - **Action**: (수정한 파일 및 내용)
      - **Outcome**: (결과 및 특이사항)
      ```