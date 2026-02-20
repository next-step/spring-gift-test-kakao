# 개발과정
## 1. claude.md 생성

## 2. 프로젝트 분석
### 1) 선물하기 기능 분석

#### 요청

```
POST /api/gifts
Header: Member-Id: 1       ← 보내는 사람
Body: {
  "optionId": 1,            ← 어떤 옵션(상품 변형)을
  "quantity": 2,             ← 몇 개
  "receiverId": 2,           ← 누구에게
  "message": "생일 축하해!"    ← 메시지
}
```

#### 처리 흐름 (6단계)

```
① GiftRestController.give()
   │  @RequestBody로 JSON 파싱, @RequestHeader("Member-Id")로 보내는 사람 ID 추출
   ▼
② GiftService.give()              ← @Transactional (여기서부터 하나의 트랜잭션)
   │
   ├─③ optionRepository.findById(optionId)
   │     → Option 엔티티 조회. 없으면 NoSuchElementException
   │
   ├─④ option.decrease(quantity)
   │     → 재고 >= 요청수량 확인. 부족하면 IllegalStateException
   │     → 통과하면 this.quantity -= quantity (재고 차감)
   │
   ├─⑤ new Gift(memberId, receiverId, option, quantity, message)
   │     → 비영속 객체 생성 (DB 저장 안 됨)
   │
   └─⑥ giftDelivery.deliver(gift)
         → FakeGiftDelivery: 보내는 사람 이름 조회 후 콘솔 출력만 수행
         → (실제 카카오 API 호출은 아직 미구현)

트랜잭션 커밋 → Option의 재고 변경이 DB에 반영됨
```

#### 성공 시

- **응답:** `200 OK` (본문 없음)
- **DB 변경:** Option 테이블의 quantity만 감소
- **콘솔 출력:** `김철수아이폰 16아이폰 16 128GB48` (이름+상품+옵션+남은수량)

#### 실패 케이스

| 상황 | 예외 | 결과 |
|------|------|------|
| 옵션이 존재하지 않음 | `NoSuchElementException` | 500 에러, 아무 변경 없음 |
| 재고 부족 | `IllegalStateException` | 500 에러, 재고 변경 없음 (롤백) |
| 보내는 사람 ID 없음 | `NoSuchElementException` (FakeGiftDelivery에서) | 500 에러, 재고 변경 없음 (롤백) |

#### 주요 특징

- **Gift 객체는 DB에 저장되지 않음** — `@Entity`가 아닌 일반 객체로, 전달 정보를 담는 용도
- **재고 차감은 JPA 더티체킹**으로 반영 — `option.decrease()` 호출만 하고 별도 `save()` 없이 트랜잭션 커밋 시 자동 UPDATE
- **GiftDelivery는 전략 패턴** — 인터페이스로 추상화되어 있고, 현재는 `FakeGiftDelivery`(콘솔 출력)만 존재
- **받는 사람(receiverId) 검증 없음** — 존재하지 않는 회원 ID를 넣어도 에러가 나지 않음 (FakeGiftDelivery가 receiver를 조회하지 않으므로)



# 프롬프트 기록

- 이 코드를 분석해서 project.md 파일을 작성해줘 
- 지금 DB 연결이랑 kakao api 설정같은게 있던데 지금 어디까지 구현됐고, 어떻게 작동되는건지 분석해서 알려줘
- 우선 선물주기 기능부터 어떤 흐름으로 작동되는지 분석해서 설명해줘
- ❯ 그럼 이 기능에서 어떤 테스트 시나리오를 만드는게 좋을까?
  예를 들어서, 재고 하나남은 상품을 2번 선물하면 두번쨰는 에러가 나는 테스트 케이스처럼 인수테스트로 작성할만한
  시나리오를 알려줘.

  검증할 행위 목록: 어떤 행위를 선택했는가? 기준은?
  테스트 데이터 전략: 어떻게 준비하고 정리하는가?
  검증 전략: 무엇을 어떻게 검증하는가?
  주요 의사결정: 논의 과정에서 중요한 선택과 이유

  위의 포맷대로 생각해서 몇 가지 알려줘
- 정리된 시나리오를 TEST_STRATEGY.md 파일로 만들어줘
- 