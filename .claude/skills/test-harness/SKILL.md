---
name: test-harness
description: 프로젝트 스택을 감지해 인수 테스트 실행 구조(서버 부팅/HTTP 클라이언트/환경설정) 뼈대를 만든다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash
---

목표:
- "API 경계에서 호출 → 결과 검증"이 가능한 테스트 하네스를 만든다.
- Cucumber/Karate 같은 BDD 도구는 사용하지 않는다.
- 가능한 한 실제 wiring(컨테이너/DI/라우팅)을 타도록 한다.

절차:
1) repo-intake 결과를 기준으로 테스트 프레임워크를 확정한다.
    - Java/Kotlin: JUnit + SpringBootTest(or Testcontainers)
    - Node: jest + supertest
    - Python: pytest + httpx/testclient
    - Go: testing + httptest
2) 아래를 만족하는 최소 예제를 제안한다.
    - 서버를 테스트에서 기동
    - HTTP 요청을 보내는 클라이언트 헬퍼
    - 테스트용 DB/스토리지 설정(가능하면 격리)
3) "한 파일로 돌아가는 샘플 테스트"를 1개 설계한다.

출력 포맷:
- 감지된 스택/테스트 프레임워크: ...
- 추천 하네스 방식(선택 이유): ...
- 생성/수정할 파일 목록(경로): ...
- 샘플 테스트 구조(의사코드 수준): ...

