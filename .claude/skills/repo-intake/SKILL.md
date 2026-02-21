---
name: repo-intake
description: 레포의 기술 스택, 실행/테스트 방법, 핵심 엔트리포인트(라우트/컨트롤러/서비스)를 빠르게 파악한다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash
---

너는 레거시 프로젝트의 인수 테스트를 준비하는 QA/BE 역할이다.
목표: "어떻게 실행하고, 어디서부터 행동이 시작되는지"를 15분 내로 파악한다.

절차:
1) Glob/Grep으로 아래 파일들을 우선 탐색해 스택/빌드/실행법을 요약한다.
    - package.json, pnpm-lock/yarn.lock
    - build.gradle/settings.gradle, pom.xml
    - requirements.txt/pyproject.toml
    - go.mod
    - docker-compose.yml, Dockerfile
    - README.md
2) 서버 진입점/라우팅 위치를 찾는다(예: controller/router/handler).
    - "route", "controller", "@RestController", "express", "fastapi", "router", "handler" 등 키워드로 Grep
3) '인수 테스트 후보가 될만한 사용자 요청'이 들어오는 지점을 5개 내로 목록화한다.
4) 결과를 아래 포맷으로 출력한다.

출력 포맷:
- 실행 방법(로컬): ...
- 테스트 프레임워크(추정): ...
- 주요 엔트리포인트: (파일경로 + 짧은 설명)
- 주요 기능 도메인 키워드: ...
- 인수 테스트 후보 엔드포인트/유즈케이스(최대 5): ...

