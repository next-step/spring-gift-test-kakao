# 사용한 프롬프트 모음 for Claude Code

```
이 프로젝트에 대한 테스트 전략 문서(TEST_STRATEGY.md)를 작성하려고 해.
현재 프로젝트 코드와 STEP1.md를 참고해서 테스트 전략 문서를 작성하기 위한 프롬프트를 작성해줘.
```


```
이 Spring Boot 프로젝트(선물하기 서비스)의 인수 테스트 전략 문서(TEST_STRATEGY.md)를 작성해줘.
단위 테스트가 아니라 행위 기반 테스트를 진행할 예정이고, 테스트 컨테이너를 띄워서 진행할거야.

## 프로젝트 컨텍스트
- 선물하기(Gift) 서비스: 카테고리/상품/옵션 관리, 위시리스트, 선물 보내기(재고 차감) 기능
- 테스트가 하나도 없는 레거시 코드
- 목표: 리팩터링 안전망으로서 "외부 행동(External Behavior)"을 보호하는 인수 테스트 작성

## 현재 API 엔드포인트
- POST /api/categories (카테고리 생성, form param)
- GET /api/categories (카테고리 목록 조회)
- POST /api/products (상품 생성, form param, categoryId 필요)
- GET /api/products (상품 목록 조회)
- POST /api/gifts (선물 보내기, JSON body + Member-Id 헤더, 재고 차감)

## 컨트롤러 없이 서비스만 존재하는 기능
- OptionService: 옵션 생성/조회 (REST endpoint 없음)
- WishService: 위시리스트 생성 (REST endpoint 없음)
- Member: CRUD API 없음 (DB 직접 생성만 가능)

## 핵심 비즈니스 규칙
- 선물 보내기 시 Option.decrease()로 재고 차감
- 재고 부족 시 IllegalStateException 발생 (현재 500 에러로 반환)
- 상품 생성 시 존재하지 않는 categoryId → NoSuchElementException
- GiftDelivery 포트를 통한 배송 처리 (현재 FakeGiftDelivery가 stdout 출력)

## 문서에 포함해야 할 섹션

### 1. 검증할 행위 목록
- 최소 5개 이상의 사용자 관점 행위를 선정
- 각 행위의 선정 기준과 이유
- 단위 테스트가 아닌 API 경계에서의 행동 기반 테스트 관점
- 성공 시나리오와 실패 시나리오 모두 고려

### 2. 테스트 데이터 전략
- H2 인메모리 DB 사용 환경에서의 데이터 준비 방법
- API를 통한 사전 데이터 준비 (Given 단계)
- 테스트 간 격리 전략 (@Transactional, @DirtiesContext, 또는 다른 방법)
- Member 엔티티처럼 API가 없는 데이터의 준비 방법

### 3. 검증 전략
- HTTP 응답 코드, 응답 본문으로 검증하는 항목
- "다음 행동"으로 이전 행동의 결과를 검증하는 전략 (예: 상품 생성 후 조회로 확인)
- DB 직접 조회 없이 API만으로 상태 변화를 확인하는 방법
- 재고 차감 같은 상태 변경의 검증 방법

### 4. 주요 의사결정
- 왜 @SpringBootTest + TestRestTemplate/MockMvc를 선택했는지
- 컨트롤러가 없는 기능(Option, Wish, Member)의 테스트 접근 방식
- 테스트 순서 의존성 vs 독립성에 대한 결정
- form param vs JSON body 같은 API 불일치 처리 방법

## 작성 시 유의사항
- BDD 도구(Cucumber, Karate 등)를 사용하지 않음
- 단위 테스트가 아닌 인수 테스트(Acceptance Test) 관점으로 작성
- "무엇을 호출했는가"가 아니라 "결과가 어떠한가"를 검증하는 방향
- 한국어로 작성
```

```
TEST_STRATEGY.md를 참고해서 필요한 내용을 CLAUDE.md로 간결하게 작성해줘.
```

```
해당 프로젝트에서 사용하면 좋을 Claude skill이 있을까?
```

```
https://code.claude.com/docs/ko/skills 이거 참고해서 skill을 만들어줘.
현재 프로젝트에 있는 모든 테스트를 실행시키고, 테스트 나온 결과를 요약해서 PDF로 만들어주는 스킬 생성해줘.
```

```
https://code.claude.com/docs/ko/skills 이거 참고해서 skill을 만들어줘.
현재 수정된 파일들을 분석하고 적절한 커밋 메시지를 생성해서 Git 커밋하는 스킬을 생성해줘.
수정된 파일이 많을 경우 변경된 내용을 그룹화해서 커밋을 여러번 해줘.
```

```
@TEST_STRATEGY.md 해당 문서를 보고 해야될 거 같은걸 해 
```

```
@TEST_REVIEW.md를 보고 수정해야 할 내용을 수정해줘
```


### 리뷰어 프롬프트
```
@REVIEWER.md 리뷰해줘. 또 결과 내용을 CODE_REVIEW.md 파일로 작성해줘 
```

```
@.claude/persona/REVIEWER.md 테스트 코드에 대해서만 리뷰하고 TEST_REVIEW.md 파일로 작성해줘 
```

