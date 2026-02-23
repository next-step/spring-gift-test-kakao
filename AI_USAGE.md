# AI 활용 방법 문서

## 사용 도구
Claude Code (claude-sonnet-4-6)

---

## 접근 방법

### 1단계: 코드베이스 분석

Claude Code의 Explore 에이전트를 통해 프로젝트 전체 구조를 파악했습니다.

**프롬프트:**
```
이 Spring Boot 프로젝트를 전체적으로 탐색해줘.
- 도메인 모델/엔티티
- REST API 엔드포인트 (컨트롤러)
- 서비스 레이어 로직
- 리포지토리 레이어
- 기존 테스트
- 의존성 (build.gradle)
- 데이터베이스 설정
```

이를 통해 다음을 빠르게 파악했습니다:
- 3개의 REST API만 존재 (Category, Product, Gift)
- Member, Option, Wish는 서비스 레이어만 있고 REST 미노출
- 테스트가 전무한 상태
- RestAssured 의존성 없음

### 2단계: 전략 설계 (AI와 협업)

미션 요구사항("External Behavior 보호", "보호 대상은 decrease() 호출이 아니라 재고 감소 결과")을 AI에게 제공하고, 설계 방향을 논의했습니다.

**핵심 논의 포인트:**
- Repository 직접 주입 vs REST API로만 데이터 준비
- "다음 행동으로 이전 행동을 검증"하는 방식
- DB 직접 조회 없이 재고 차감을 증명하는 방법

**AI의 판단:**
Repository 직접 주입은 영속성 레이어 구현에 결합되므로, REST API 전용 방식이 미션 원칙("시스템 경계에서 검증")에 더 부합한다고 결정했습니다.

### 3단계: 구현

AI가 직접 다음을 구현했습니다:

1. **build.gradle** - RestAssured 의존성 추가
2. **CreateMemberRequest, MemberService, MemberRestController** - 누락된 Member REST 레이어 추가
3. **OptionRestController** - 누락된 Option REST 레이어 추가
4. **AcceptanceTest** - 기반 클래스 (RANDOM_PORT, DB 격리)
5. **CategoryAcceptanceTest** - 행위 1, 2
6. **ProductAcceptanceTest** - 행위 3, 4
7. **GiftAcceptanceTest** - 행위 5, 6, 7, 8

구현 후 `./gradlew test` 실행으로 전체 테스트 통과 확인.

---

## 효과적이었던 활용 방법

**1. 코드 읽기 작업 위임**
프로젝트 파일이 10개 이상인 상황에서 AI가 전체를 탐색하고 요약해주어, 중요 파일에만 집중할 수 있었습니다.

**2. 설계 근거 문서화 요청**
단순히 코드를 작성하는 것이 아니라, "왜 이 결정을 내렸는가"를 TEST_STRATEGY.md에 남기도록 지시했습니다.

**3. 제약 조건과 원칙을 함께 제공**
미션의 핵심 메시지("보호 대상은 decrease() 호출이 아니라 재고 감소 결과")를 AI에게 명시적으로 전달하여, AI가 단순한 코드 생성이 아니라 원칙에 맞는 설계를 하도록 유도했습니다.

---

## 한계 및 주의사항

- AI가 생성한 테스트가 실제로 의도한 행위를 검증하는지는 사람이 직접 확인해야 합니다.
- 특히 실패 케이스(500 응답)가 **실제로 재고 보호 때문에 실패하는지**, 아니면 다른 이유로 실패하는지는 코드를 직접 읽고 확인했습니다.