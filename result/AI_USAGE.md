# AI 활용 방법 문서

## 1. 사용한 AI 도구

| 도구 | 버전 | 용도 |
|------|------|------|
| **Claude Code** | CLI | 코드 생성, 테스트 실행, 문서 작성 |
| **oh-my-claudecode (OMC)** | v4.2.14 | 스킬 자동화, 워크플로우 관리 |

---

## 2. 정의한 커스텀 스킬 (Skills)

프로젝트 전용 5개의 스킬을 `.claude/commands/` 디렉토리에 정의했습니다.

### 2.1 `/test` - 테스트 실행

**파일:** `.claude/commands/test-forKakaoOnboarding.md`

```markdown
# /test-forKakaoOnboarding

선물하기 시스템 인수 테스트를 실행합니다.

## 즉시 실행

```bash
./gradlew test --tests "gift.GiftAcceptanceTest" 2>&1
```

실행 후 결과를 분석해서 다음 형식으로 보고해:

```
🎁 선물하기 테스트 결과

[PASSED/FAILED] 행위 1: 선물하기 시 재고 감소
[PASSED/FAILED] 행위 2: 누적 재고 감소
[PASSED/FAILED] 행위 3: 재고 부족 시 실패
[PASSED/FAILED] 행위 4: 재고 소진 후 실패
[PASSED/FAILED] 행위 5: 잘못된 옵션 실패

결과: N/5 통과
```

실패한 테스트가 있으면 build/test-results/test/*.xml 파일에서
실패 원인을 추출해서 알려줘.
```

**사용 예:**
```
> /test
🎁 선물하기 테스트 결과
✅ PASSED 행위 1: 선물하기 시 재고 감소
...
결과: 5/5 통과
```

---

### 2.2 `/test-fix` - 실패 테스트 자동 수정

**파일:** `.claude/commands/test-fix-forKakaoOnboarding.md`

```markdown
# /test-fix-forKakaoOnboarding

실패한 테스트를 자동으로 분석하고 수정합니다.

## 1단계: 테스트 실행 및 실패 확인
```bash
./gradlew test --tests "gift.GiftAcceptanceTest" 2>&1
```

## 2단계: 실패 시 원인 분석
```bash
cat build/test-results/test/*.xml 2>/dev/null | grep -A 20 "failure"
```

## 3단계: 패턴별 자동 수정

실패 원인에 따라 다음 수정을 적용해:

### Member 조회 실패 (NoSuchElementException at FakeGiftDelivery)
→ GiftAcceptanceTest.java의 @BeforeEach에 다음 추가:
```java
sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
```

### 데이터 격리 실패 (expected: N but was: M)
→ 클래스에 다음 어노테이션 추가:
```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

### DTO 바인딩 실패 (The given id must not be null)
→ API 호출을 JSON body 방식으로 변경하거나 Request DTO에 setter 추가

## 4단계: 수정 후 재실행
```bash
./gradlew test --tests "gift.GiftAcceptanceTest" 2>&1
```

통과할 때까지 2-4단계를 반복해.
```

---

### 2.3 `/test-generate` - 새 테스트 생성

**파일:** `.claude/commands/test-generate-forKakaoOnboarding.md`

```markdown
# /test-generate-forKakaoOnboarding

선물하기 시스템의 새 테스트를 자동 생성합니다.

## 현재 테스트 위치
`src/test/java/gift/GiftAcceptanceTest.java`

## 생성할 테스트 선택

사용자에게 물어봐:
1. 동시성 테스트 (같은 옵션 동시 선물)
2. 입력 검증 테스트 (음수/0 수량)
3. 비즈니스 규칙 테스트 (자기 자신에게 선물)

## 테스트 코드 템플릿

GiftAcceptanceTest.java 파일을 읽고, 기존 구조를 따라서 새 @Nested 클래스를 추가해:

```java
@Nested
@DisplayName("행위 N: [행위 설명]")
class [ClassName] {

    @Test
    @DisplayName("[한글 설명]")
    void [methodName]() {
        // given
        Option option = createOptionWithStock(N);

        // when
        ResponseEntity<Void> response = sendGift(option.getId(), M);

        // then
        assertThat(response.getStatusCode())...;
        assertThat(getStock(option.getId()))...;
    }
}
```

## 생성 후 검증
```bash
./gradlew test --tests "gift.GiftAcceptanceTest" 2>&1
```

새 테스트가 통과하는지 확인하고, 실패하면 `/test-fix` 실행해.
```

---

### 2.4 `/test-analyze` - 테스트 품질 분석

**파일:** `.claude/commands/test-analyze-forKakaoOnboarding.md`

```markdown
# /test-analyze-forKakaoOnboarding

현재 테스트 코드의 품질을 분석합니다.

## 1단계: 테스트 파일 읽기
`src/test/java/gift/GiftAcceptanceTest.java` 파일을 읽어.

## 2단계: 체크리스트 검증

### 핵심 행위 커버리지 (현재 5개)
- [ ] 선물하기 시 재고 감소
- [ ] 누적 재고 감소
- [ ] 재고 부족 시 실패
- [ ] 재고 소진 후 실패
- [ ] 존재하지 않는 옵션으로 실패

### 누락된 엣지 케이스
- [ ] 동시성 (동시에 같은 옵션 선물)
- [ ] 음수 수량
- [ ] 0 수량
- [ ] 자기 자신에게 선물
- [ ] 메시지 빈값

### 테스트 구조
- [ ] @Nested로 행위별 그룹화
- [ ] @DisplayName 한글 설명
- [ ] Given-When-Then 주석
- [ ] Fixture 메서드 분리 (createOptionWithStock, getStock)
- [ ] API Helper 분리 (sendGift)

### 데이터 격리
- [ ] Testcontainers + TRUNCATE CASCADE 사용
- [ ] 각 테스트 독립 실행 가능

## 3단계: 결과 출력

```
📊 테스트 품질 분석

| 항목 | 점수 | 비고 |
|------|------|------|
| 핵심 행위 | N/5 | |
| 엣지 케이스 | N/5 | |
| 테스트 구조 | N/5 | |
| 데이터 격리 | N/5 | |

총점: N/20

개선 제안:
1. ...
2. ...
```
```

---

### 2.5 `/qa` - 전체 QA 파이프라인

**파일:** `.claude/commands/qa-forKakaoOnboarding.md`

```markdown
# /qa-forKakaoOnboarding

선물하기 시스템 전체 QA를 자동으로 실행합니다.

## 파이프라인 실행

### Stage 1: 빌드 확인
```bash
./gradlew compileJava compileTestJava 2>&1
```
실패하면 에러 메시지 분석하고 수정해.

### Stage 2: 테스트 실행
```bash
./gradlew test --tests "gift.GiftAcceptanceTest" 2>&1
```

### Stage 3: 결과 확인
- 전체 통과 → Stage 5로
- 실패 있음 → Stage 4로

### Stage 4: 자동 수정 (실패 시)
`/test-fix` 내용대로 수정하고 Stage 2 재실행.
최대 3회 반복.

### Stage 5: 최종 리포트

```
🎁 선물하기 시스템 QA 완료

빌드: ✅ 성공
테스트: ✅ 5/5 통과

검증된 행위:
1. ✅ 선물하기 시 재고 감소
2. ✅ 누적 재고 감소
3. ✅ 재고 부족 시 실패
4. ✅ 재고 소진 후 실패
5. ✅ 잘못된 옵션 실패

프로덕션 코드 변경: 없음
테스트 파일: src/test/java/gift/GiftAcceptanceTest.java
```

## 종료 조건
- 모든 테스트 통과
- 또는 3회 수정 시도 후에도 실패 시 사용자에게 보고
```

---

## 3. 사용한 프롬프트 및 접근 방법

### 3.1 테스트 코드 생성 프롬프트

```
선물하기 시스템의 인수 테스트를 작성해줘.

요구사항:
1. 5가지 핵심 행위를 검증
   - 선물 시 재고 감소
   - 누적 재고 감소
   - 재고 부족 시 실패
   - 재고 소진 후 실패
   - 잘못된 옵션으로 실패

2. 테스트 구조
   - @Nested로 행위별 그룹화
   - @DisplayName 한글 설명
   - Given-When-Then 패턴

3. 데이터 격리
   - @DirtiesContext 사용
   - Fixture 메서드 분리
```

### 3.2 스킬 정의 프롬프트

```
프로젝트 전용 테스트 자동화 스킬을 만들어줘.

필요한 스킬:
1. /test - 테스트 실행 및 결과 포맷팅
2. /test-fix - 실패 테스트 자동 분석/수정
3. /test-generate - 새 테스트 템플릿 생성
4. /test-analyze - 테스트 품질 분석
5. /qa - 전체 파이프라인 (빌드→테스트→수정→리포트)

각 스킬은 .claude/commands/ 디렉토리에 마크다운으로 저장해.
```

### 3.3 반복 개선 프롬프트

```
/test 실행 후 실패한 테스트가 있어.
실패 원인을 분석하고 자동으로 수정해줘.
수정 후 다시 테스트를 실행해서 통과할 때까지 반복해.
```

---

## 4. AI 활용 워크플로우

```
┌─────────────────────────────────────────────────────────────┐
│                    AI 활용 개발 사이클                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 요구사항 분석                                           │
│      └─→ "선물하기 시스템의 핵심 행위 5가지를 정의해줘"        │
│                                                             │
│   2. 테스트 코드 생성                                        │
│      └─→ "행위별로 @Nested 구조로 테스트 작성해줘"            │
│                                                             │
│   3. 실행 및 검증                                            │
│      └─→ /test (커스텀 스킬)                                 │
│                                                             │
│   4. 실패 시 자동 수정                                       │
│      └─→ /test-fix (커스텀 스킬)                             │
│                                                             │
│   5. 품질 분석                                               │
│      └─→ /test-analyze (커스텀 스킬)                         │
│                                                             │
│   6. 전체 QA                                                 │
│      └─→ /qa (커스텀 스킬)                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. 스킬 파일 구조

```
.claude/
└── commands/
    ├── test-forKakaoOnboarding.md        # 테스트 실행
    ├── test-fix-forKakaoOnboarding.md    # 자동 수정
    ├── test-generate-forKakaoOnboarding.md # 테스트 생성
    ├── test-analyze-forKakaoOnboarding.md  # 품질 분석
    └── qa-forKakaoOnboarding.md          # 전체 QA
```

---

## 6. CLAUDE.md 프로젝트 설정

```markdown
# 선물하기 시스템 (Spring Gift)

## 프로젝트 개요
카카오 스타일 선물하기 시스템의 백엔드 API

## 기술 스택
- Java 21
- Spring Boot 3.5
- Spring Data JPA
- PostgreSQL 16 (Testcontainers)

## 도메인 구조
Category → Product → Option → Gift
                      ↓
                   (재고 관리)

## 핵심 비즈니스 규칙
1. 선물 시 옵션의 재고가 감소한다
2. 재고보다 많은 수량은 선물할 수 없다
3. 재고가 0이면 선물할 수 없다

## 테스트 명령어
| 명령어 | 설명 |
|--------|------|
| `/test` | 전체 테스트 실행 |
| `/test-fix` | 실패 테스트 자동 수정 |
| `/test-analyze` | 테스트 품질 분석 |
| `/test-generate` | 테스트 자동 생성 |
| `/qa` | 전체 QA 파이프라인 |
```

---

## 7. 효과 및 학습

### AI 활용의 장점

| 영역 | 효과 |
|------|------|
| **코드 생성** | 보일러플레이트 코드 자동 생성으로 시간 절약 |
| **패턴 적용** | Given-When-Then, Nested 구조 일관성 유지 |
| **자동 수정** | 실패 원인 분석 및 수정 자동화 |
| **문서화** | 테스트 전략, 사용 방법 문서 자동 생성 |
| **인프라 구축** | Dockerfile, Docker Compose, Cucumber BDD 자동 설정 |

### 스킬 기반 자동화의 이점

1. **재사용성**: 한 번 정의한 스킬을 반복 사용
2. **일관성**: 동일한 형식의 결과 출력
3. **효율성**: 복잡한 명령어를 간단한 슬래시 명령으로 실행
4. **학습 곡선 감소**: 프로젝트 특화 워크플로우 캡슐화

---

## 8. Step 2: 인수 테스트 체계 고도화

### 8.1 Autopilot 워크플로우

```
Phase 0: Expansion (요구사항 분석)
  ├── Analyst: 요구사항 추출
  ├── Architect: 기술 명세 작성
  └── Explorer: 코드베이스 분석

Phase 1: Planning (구현 계획)
Phase 2: Execution (3단계 구현)
  ├── Step 1: Cucumber BDD (Korean Gherkin + RestAssured)
  ├── Step 2: Docker Compose PostgreSQL
  └── Step 3: Application Containerization
Phase 3: QA (빌드 + 테스트 검증)
Phase 4: Validation (아키텍트/보안/코드 리뷰)
```

### 8.2 단계적 커밋 전략

| 커밋 | 내용 | 검증 |
|------|------|------|
| `feat: Cucumber BDD 테스트 추가` | Korean Gherkin + RestAssured + Testcontainers | 7 시나리오 통과 |
| `feat: Docker Compose PostgreSQL 통합` | docker-compose.yml + cucumber 프로필 | 7 시나리오 통과 |
| `feat: 애플리케이션 컨테이너화` | Multi-stage Dockerfile + E2E 테스트 | 7 시나리오 통과 |

### 8.3 AI 프롬프트

```
/autopilot
1. 단계적 커밋을 할 것
2. 로컬도 container도 모두 postgres로 진행할 것
3. 완전한 컨테이너화 한 후 검증까지 마칠 것
```

### 8.4 기술 스택 추가

| 기술 | 버전 | 용도 |
|------|------|------|
| Cucumber | 7.22.0 | BDD 테스트 프레임워크 |
| RestAssured | (Spring Boot managed) | HTTP API 테스트 |
| Docker Compose | 5.0.2 | 컨테이너 오케스트레이션 |
| Spring Boot Actuator | (Spring Boot managed) | 컨테이너 헬스체크 |
