
# Claude Code(AI) 활용 기반 E2E 테스트 인프라 구축 방법 정리

본 문서는 **선물하기(Gift) 서비스 프로젝트**에서 Claude Code를 활용해 **Cucumber BDD 테스트 → PostgreSQL Docker 전환 → Application 컨테이너화**까지 구축한 과정과 실제 사용한 프롬프트를 정리한 문서입니다.

---

## 1. 전체 진행 흐름

| 단계 | 주요 작업 | 관련 스킬 |
| --- | --- | --- |
| **1단계** | Cucumber BDD 테스트 작성 | `/acceptance-test-writer` |
| **2단계** | H2 → PostgreSQL + Docker Compose 전환 | `/postgres-docker-setup` |
| **3단계** | App Docker 컨테이너화 | `/app-containerization` |

---

## 2. 1단계 → 2단계 전환에 사용한 프롬프트

```markdown
Q. https://edu.nextstep.camp/s/fkNoSONS/ls/4E1Dy593를 기반으로 요구사항 1까지는 마쳤어.
요구사항 2까지 마치기 위한, CLAUDE.md를 수정해줘
```
→ CLAUDE.md에 2단계 목표와 변경 금지 사항 추가

```markdown
Q. 이를 기반으로, 요구사항 2를 위한 스킬을 만들어줘.
```
→ `/postgres-docker-setup` 스킬 생성

```markdown
Q. 이제 CLAUDE.md에 꼭 남겨야하는 내용만 남겨도 되겠지?
```
→ CLAUDE.md와 스킬 간 중복 제거

```markdown
Q. CLAUDE.md와 postgres-docker-setup 스킬이 요구사항 1단계에서 2단계로 넘어가기 위해
잘 작성되었는지 검증하시오. 잘못되었다면 수정하고, 더 좋은 방법이 있다면 개선하시오.
```
→ 요구사항 원문과 대조 검증 후 보완

```markdown
Q. GiftSteps.java 과 CommonSteps.java 에 해당하는 작업만 진행해줘.
```
→ PostgreSQL 연동 완료 (Gift, Product, Category 모두 `TRUNCATE ... CASCADE`로 격리)

---

## 3. 2단계 → 3단계 전환에 사용한 프롬프트

```markdown
Q. https://edu.nextstep.camp/s/fkNoSONS/ls/4E1Dy593를 기반으로 요구사항 2까지는 마쳤어.
요구사항 3까지 마치기 위한, CLAUDE.md를 수정해줘
```
→ 3단계 목표 + 아키텍처 다이어그램 추가

```markdown
Q. CLAUDE.md를 기반으로 skills를 수정하고, 중복되는 내용은 CLAUDE.md에서 제거해줘.
```
→ `/app-containerization` 스킬 생성, CLAUDE.md 간소화

```markdown
Q. CLAUDE.md와 스킬이 요구사항 2단계에서 3단계로 넘어가기 위해
잘 작성되었는지 검증하시오. 잘못되었다면 수정하고, 더 좋은 방법이 있다면 개선하시오.
```
→ 요구사항 원문과 대조 검증 후 보완

```markdown
Q. 요구사항 3단계에 대한 업무를 진행해줘
```
→ Dockerfile, docker-compose.yml(app 추가), CucumberSpringConfiguration(`NONE` + 28080 고정), build.gradle(docker 태스크 추가) 등 수정 완료

---

## 4. 매 단계 반복한 프로세스

  1. 요구사항 URL에서 다음 단계 파악
  2. **CLAUDE.md 수정** (목표, 변경 금지 사항, 수정 대상)
  3. **스킬 생성/수정** + CLAUDE.md 중복 제거
  4. **요구사항 원문 대조 검증**
  5. **작업 실행** 및 테스트 확인
