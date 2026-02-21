---
name: acceptance-workflow
description: 레거시 인수 테스트 미션을 단계별로 진행하도록 체크리스트와 다음 실행 명령을 안내한다.
disable-model-invocation: true
---

목표: 프로젝트 인수 테스트 미션을 4단계로 진행한다.

1) /repo-intake 실행 → 결과 요약
2) /core-behaviors 실행 → Top5 행동 선정
3) 각 행동마다 /behavior-trace → /acceptance-scenarios → /test-data-plan → /behavior-assert
4) /test-harness로 1개 테스트를 먼저 통과시키고 확장
5) /docs-packager로 제출 문서 초안 생성

사용자에게 다음에 실행할 슬래시 명령을 항상 제시하라.

