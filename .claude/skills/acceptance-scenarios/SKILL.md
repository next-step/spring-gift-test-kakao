---
name: acceptance-scenarios
description: BDD 도구 없이 Given/When/Then 형태로 인수 테스트 시나리오(성공/실패)를 설계한다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob
---

입력: $ARGUMENTS (행동 이름 또는 엔드포인트)

목표:
- '행동 1개'당 1~3개 시나리오(성공 1 + 중요한 실패 1~2) 설계
- 검증은 "응답 + 최종 상태" 중심
- UI/구현 세부사항 변화에 덜 민감하게

절차:
1) Given: 필요한 초기 상태를 최소로 정의(계정, 상품, 재고, 잔액 등)
2) When: 사용자 요청(HTTP/CLI)을 정의
3) Then: 기대 결과를 "관찰 가능한 것"으로 정의
    - HTTP 응답(코드/바디 일부)
    - DB 최종 상태(핵심 필드)
    - 예외 타입/에러 코드
    - 후속 행동으로 검증(가능하면 DB 직접 조회를 줄이기)
4) 각 시나리오에 "데이터 준비 방법"과 "정리(cleanup) 방식"을 붙인다.

출력 포맷:
- 시나리오 목록:
    - [성공] Given ... When ... Then ...
    - [실패] Given ... When ... Then ...
- 데이터 준비/정리 메모: ...
- 테스트에서 꼭 피할 것(예: 내부 메서드 호출 검증): ...

