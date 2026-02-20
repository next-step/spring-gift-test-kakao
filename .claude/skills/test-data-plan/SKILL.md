---
name: test-data-plan
description: 인수 테스트 데이터 준비/정리(초기상태, 격리, cleanup, fixture/builder)를 설계하고 헬퍼 방향을 제시한다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob
---

목표:
- 테스트끼리 독립(격리)되게 만들고,
- "Given 상태를 만들기 쉬운" 데이터 전략을 확정한다.

검토할 옵션:
A) API로 준비(시드용 엔드포인트/어드민 기능이 있으면 최우선)
B) DB 직접 준비(최후의 수단: 최소 테이블/필드만)
C) Factory/Builder로 도메인 객체 생성(단, 내부 구현 결합 주의)

절차:
1) 각 시나리오의 Given에 필요한 최소 엔티티를 나열한다.
2) 데이터 생성/정리 방식을 하나로 통일한다(혼합 시 규칙 명시).
3) 테스트 격리 전략:
    - 트랜잭션 롤백? 컨테이너 재사용? DB 스키마 리셋? unique namespace?
4) cleanup 규칙을 작성한다.

출력 포맷:
- 데이터 준비 원칙(선택한 옵션 + 이유): ...
- 공통 fixture/builder 목록: ...
- 격리 전략(테스트 간): ...
- 정리(cleanup) 전략: ...

