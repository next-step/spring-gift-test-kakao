# AI 활용 기록

## 사용 도구
- Claude Code (Anthropic Claude)

## 활용 내역

### 1. CLAUDE.md 작성

Ask AI를 통해 분석한 CLAUDE.md의 best practice에 따르면 CLAUDE.md는 일회성 작업 지시가 아니라 프로젝트 규칙과 컨벤션을 간결하게 담는 파일이어야 한다.

AI에게 공식 문서 기준으로 검토를 요청했고, 우리가 작성한 파일에서 다음 문제를 발견하여 수정했다.

- 114줄 작업 프롬프트 → 30줄 프로젝트 규칙 (빌드 명령, 코드 스타일, 테스트 컨벤션, 커밋 규칙)
- "이렇게 해줘" 형태 → "이 프로젝트에서는 이렇게 한다" 형태

이렇게 정리하니 이후 AI가 테스트를 작성할 때 별도 설명 없이도 `@Sql` 사용, RestAssured 기반 검증, AngularJS 커밋 스타일 등 프로젝트 규칙을 자동으로 따랐다.

### 2. Claude Code Skill 활용

인수 테스트를 반복적으로 작성하는 패턴을 `/write-acceptance-test` 스킬로 만들었다. 스킬은 4단계로 구성된다.

1. **테스트 전략 수립**: 검증할 행위 정의, 엔드포인트 확인, 검증 방법 결정
2. **테스트 데이터 준비**: FK 관계 파악, SQL INSERT문 작성
3. **테스트 메서드 작성**: RestAssured 요청 구성, 응답 + DB 상태 검증
4. **검증**: `./gradlew test` 실행

스킬에 실제 코드베이스의 예시("선물을 보내면 옵션 재고가 감소한다")를 포함시켜서, AI가 프로젝트 패턴을 그대로 따라 새 테스트를 작성할 수 있게 했다. `/write-acceptance-test 위시리스트 추가 시 조회에 포함된다`처럼 행위를 인자로 넘기면 스킬이 작동한다.

### 3. 테스트 코드 작성 과정

AI에게 프로젝트 구조 분석을 먼저 맡겼다. 이 과정에서 `CategoryRestController`와 `ProductRestController`에 `@RequestBody`가 없고 DTO에 setter도 없어서 form parameter 바인딩이 동작하지 않는 문제를 발견했다. AI가 먼저 POST+GET 조합 테스트를 작성했다가 실패한 뒤, 원인을 분석하고 `@Sql` 기반 데이터 준비 + GET 검증 방식으로 전환했다.

테스트 데이터 관리도 Repository 직접 호출에서 `@Sql` 방식으로 변경했는데, 이는 선언적 데이터 관리와 H2 인스턴스 재활용을 위해서였다. AI가 cleanup.sql과 테스트별 data.sql 구조를 설계했고, `@Sql` 애노테이션으로 매 테스트 전 자동 실행되도록 구성했다.

### 4. AI가 틀렸던 부분과 보완

- **POST 바인딩 오류**: 처음에 form param으로 Category/Product POST 테스트를 작성했으나 DTO에 setter가 없어 실패. `./gradlew test` 실행 결과와 에러 로그를 분석하여 원인을 파악하고 접근 방식을 수정했다.
- **CLAUDE.md 형식**: 기존 작업 프롬프트를 CLAUDE.md로 그대로 사용하려 했으나, best practice 검토를 요청하여 공식 문서 기준으로 전면 재작성했다.

AI 출력은 항상 `./gradlew test` 실행으로 검증했고, 실패하면 에러 로그를 읽고 수정하는 사이클을 반복했다.
