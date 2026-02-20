---
name: behavior-assert
description: 인수 테스트에서 '무엇을 어떻게 검증할지' 규칙을 만들고, 재사용 가능한 assertion/checklist를 제시한다.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob
---

목표:
- 내부 호출(verify decrease 호출 등) 대신,
- 외부 관찰 가능 결과로 검증하도록 팀 규칙을 만든다.

검증 우선순위(권장):
1) HTTP 응답(코드 + 계약에 중요한 필드)
2) 최종 상태(핵심 도메인 결과: 주문 생성/잔액 감소/재고 변경)
3) 실패 시 예외 타입/에러 코드/메시지 규칙(계약으로서)
4) 가능하면 "후속 행동"으로 이전 행동을 검증(DB 직접 조회를 줄임)

절차:
1) 선정된 행동 5개 각각에 대해 "검증 포인트 체크리스트"를 만든다.
2) 공통 assertion 헬퍼 후보를 뽑는다.
    - 예: assertInventory(productId, expectedQty)
    - 예: assertBalance(userId, expected)
    - 예: assertOrderCreated(requestId)
3) DB 직접 조회가 필요하면 "허용 범위"를 문서화한다(핵심 테이블만, 읽기만).

출력 포맷:
- 공통 검증 원칙: ...
- 행동별 체크리스트(5개 이상): ...
- 공통 assertion 헬퍼 후보: ...
- DB 조회 허용 범위(있다면): ...

