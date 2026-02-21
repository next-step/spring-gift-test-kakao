# AI_USAGE.md — Claude Code 활용 기록

## 1. 사용 도구

| 도구 | 용도 |
|------|------|
| **Claude Code (CLI)** | 코드 분석, 테스트 설계, 테스트 코드 작성, 문서 생성 |
| **모델** | Claude Opus 4.6 |

---

## 2. 프로젝트 핵심 기능 파악에 사용한 프롬프트

### 2-1. 저장소 전체 스캔

```
Thoroughly explore the project. I need:
1. Full directory tree (all source files, test files, config files)
2. Build tool used (Gradle/Maven) and key dependencies
3. All controller/router/handler endpoints (REST API paths)
4. All entity/domain classes and their relationships
5. All service classes and their key methods
6. Any existing test files and what they test
7. Database schema or migration files if any
8. Application configuration (application.yml/properties)
```

**결과:** 프로젝트 전체 구조 파악. 3개 Controller, 5개 Service, 5개 Entity, 5개 Repository 식별.

### 2-2. API 엔드포인트 추출

Controller 파일을 직접 읽어 `@RequestMapping`, `@PostMapping`, `@GetMapping` 어노테이션과 파라미터 바인딩 방식(`@RequestBody` 유무)을 확인했다.

**발견:** `CategoryRestController`와 `ProductRestController`의 POST 메서드에 `@RequestBody`가 없어 form/query 파라미터 바인딩을 사용하는데, DTO 클래스(`CreateCategoryRequest`, `CreateProductRequest`)에 setter가 없어 바인딩이 실패하는 버그를 발견했다.

### 2-3. 핵심 흐름 추적

```
GiftService.give() 메서드의 실행 흐름을 추적해줘:
1. Option 조회 → decrease() → Gift 생성 → deliver() 순서
2. 트랜잭션 경계와 롤백 시나리오
3. FakeGiftDelivery에서 member 조회가 실패하면?
```

**결과:** `option.decrease()` → `giftDelivery.deliver()` 순서로, decrease 이후 deliver에서 실패하면 트랜잭션 롤백이 재고를 원복해야 한다는 핵심 위험 지점 식별.

---

## 3. 행위 선정에 사용한 접근법

### 3-1. 외부 행동 후보 추출 체크리스트

CLAUDE.md의 "Project Scanning Checklist"를 기반으로:

1. Controller → Service → Repository 흐름 추적
2. "상태 변화가 발생하는 행동" 우선 추출 (CUD > Read)
3. 트랜잭션/영속성/예외 관련 위험 구간 식별
4. 실패 시 비즈니스 영향 평가

**결과:** 11개 후보 중 7개 선정 (`행위리스트.md` → `TEST_STRATEGY.md`)

### 3-2. 선정 기준 프롬프트

```
후보 행위들을 아래 기준으로 우선순위를 매겨줘:
1. 상태 변화가 있는 흐름 (CUD) 우선
2. 리팩터링 위험이 큰 구간 (트랜잭션/영속성/예외) 우선
3. 실패 시 비즈니스 영향이 큰 시나리오 우선
```

---

## 4. 테스트 설계에 사용한 체크리스트

### 4-1. 각 행위별 설계 템플릿

```
Behavior N에 대해:
- Given: 초기 상태/데이터
- When: API 호출 (HTTP 메서드, 경로, 헤더, 바디)
- Then: 검증 항목 (HTTP 응답 + 상태 변화)
- 검증 방법: 조회 API vs Repository 직접 조회
```

### 4-2. 테스트 데이터 전략 결정

```
데이터 준비 방식을 결정해줘:
- Repository 호출 vs @Sql SQL 파일
- @Transactional 롤백 vs TRUNCATE 기반 격리
- MockMvc vs TestRestTemplate
각 선택지의 장단점과 이 프로젝트에 적합한 이유를 설명해줘.
```

**결정:**
- `@Sql` SQL 파일 (리팩터링 내성)
- TRUNCATE 기반 격리 (실제 트랜잭션 커밋/롤백 검증)
- TestRestTemplate (실제 HTTP 경로)

---

## 5. 테스트 코드 작성에 사용한 프롬프트

### 5-1. 첫 번째 테스트 (B1: 선물 성공 → 재고 감소)

```
Behavior 1 테스트를 작성해줘:
- @SpringBootTest(RANDOM_PORT) + TestRestTemplate
- @Sql로 데이터 준비 (gift-setup.sql)
- POST /api/gifts 호출 후 HTTP 200 확인
- OptionRepository로 재고 감소 확인 (10 → 7)
```

### 5-2. 트랜잭션 롤백 검증 테스트 (B2, B7)

```
B2 (재고 부족)와 B7 (회원 미존재) 테스트에서 트랜잭션 롤백을 검증해줘:
- 실패 시 HTTP 500 확인
- DB에서 Option quantity가 원래 값으로 유지되는지 확인
- @Transactional 테스트가 아닌 TRUNCATE 기반이므로 실제 커밋/롤백 동작 확인 가능
```

### 5-3. DTO 바인딩 버그 테스트 (B4)

```
ProductRestController의 DTO 바인딩 버그를 테스트로 문서화해줘:
- CreateProductRequest에 setter가 없어서 query param 바인딩 실패
- categoryId가 null → findById(null) → IllegalArgumentException → HTTP 500
- 현재 동작(current behavior)을 그대로 테스트로 고정
```

---

## 6. 반복적으로 유효했던 프롬프트 템플릿

### 템플릿 1: 행위 추적

```
[서비스 메서드]의 실행 흐름을 추적해줘:
1. 어떤 순서로 어떤 메서드를 호출하는가?
2. 트랜잭션 경계는 어디인가?
3. 실패 시 어떤 예외가 발생하고 롤백은 보장되는가?
```

### 템플릿 2: 테스트 코드 생성

```
[행위 이름] 테스트를 작성해줘:
- Given: [데이터 준비 방식]
- When: [API 호출 상세]
- Then: [검증 항목 + 방법]
- 주의: 외부 행동만 검증. Mock verify 사용 금지.
```

### 템플릿 3: 워크플로우 실행

```
/acceptance-workflow를 실행해줘:
1. /repo-intake → 결과 요약
2. /core-behaviors → Top5 행동 선정
3. 각 행동마다 테스트 작성
4. 전체 테스트 통과 확인
5. 제출 문서 생성
```

---

## 7. 주요 발견사항

### AI가 발견한 코드 이슈

1. **DTO 바인딩 버그**: `CreateProductRequest`, `CreateCategoryRequest`에 setter 없음
   - form/query 파라미터 바인딩 시 필드가 null로 남음
   - 이를 테스트로 문서화하여 리팩터링 시 인지할 수 있게 함

2. **트랜잭션 원자성 위험**: `GiftService.give()`에서 `option.decrease()` → `giftDelivery.deliver()` 순서
   - deliver 실패 시 decrease가 롤백되어야 하는데, `@Transactional` 제거 시 위험
   - B7 테스트로 이 위험을 보호

3. **JPA 더티 체킹 의존**: `option.decrease()` 후 명시적 `save()` 없음
   - 영속성 컨텍스트가 변경되면 (예: `clear()` 추가) 재고 감소가 누락될 수 있음
   - B1 테스트로 이 동작을 보호

---

## 8. 워크플로우 요약

| 단계 | 내용 | 소요 시간 |
|------|------|-----------|
| 1. 저장소 스캔 | 전체 구조 파악, API/도메인/서비스 식별 | ~2분 |
| 2. 행위 추출 | 11개 후보 → 7개 선정 | ~3분 |
| 3. 테스트 전략 수립 | 데이터/격리/검증 방식 결정 | ~5분 |
| 4. 테스트 코드 작성 | 7개 행위 테스트 (3 파일) | ~10분 |
| 5. 테스트 실행/검증 | `./gradlew test` 통과 확인 | ~1분 |
| 6. 문서화 | TEST_STRATEGY.md, AI_USAGE.md | ~5분 |
