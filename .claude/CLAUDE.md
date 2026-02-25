# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 한 줄 요약

카카오 메시지 API를 활용한 선물하기 서비스 (Spring Boot 기반)

## 빌드 및 실행 명령어

```bash
./gradlew build          # 프로젝트 빌드
./gradlew test           # 단위 테스트 실행
./gradlew cucumberTest   # 인수 테스트 실행 (PostgreSQL 자동 시작)
./gradlew dockerBuild    # Docker 이미지 빌드
./gradlew dockerUp       # Docker Compose 전체 기동
./gradlew dockerDown     # Docker Compose 종료
./gradlew bootRun        # 애플리케이션 로컬 실행
```

- Java 21, Gradle 8.4, Spring Boot 3.5.8

## 아키텍처

`gift/` 패키지 하위 3계층 구조:

- **`ui`** — REST 컨트롤러 (`/api/products`, `/api/categories`, `/api/gifts`)
- **`application`** — 서비스 (@Transactional 비즈니스 로직) 및 요청 DTO
- **`model`** — JPA 엔티티, Spring Data 리포지토리, 도메인 인터페이스 (`GiftDelivery`)
- **`infrastructure`** — 외부 연동 구현체 (`FakeGiftDelivery`, 카카오 API 프로퍼티)

의존성 방향: `ui → application → model` ← `infrastructure`

## 핵심 도메인 모델

- **Product** — Category에 속하며, 여러 Option을 가짐
- **Option** — 상품 변형. 수량(quantity) 관리, `decrease()`로 재고 차감
- **Member** — id, name, email. Wish(위시리스트)를 가짐
- **Gift** — 비영속 객체(JPA 엔티티 아님). Option, 보내는/받는 사람 ID, 수량, 메시지를 담음

### 주요 API

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/gifts` | POST | 선물 보내기 (`Member-Id` 헤더로 보내는 사람 식별) |
| `/api/products` | GET/POST | 상품 목록 조회 / 등록 |
| `/api/categories` | GET/POST | 카테고리 목록 조회 / 등록 |

## 설정

- `@ConfigurationPropertiesScan`으로 `KakaoMessageProperties`, `KakaoSocialProperties` 로드
- `spring.jpa.open-in-view=false` — 트랜잭션 외부에서 지연 로딩 불가

## 개발 규칙

### 리팩터링 원칙

외부 행동(external behavior)은 절대 변경하지 않는다.

- API 응답 구조, HTTP 상태 코드, 예외 타입 유지
- 비즈니스 결과와 DB 최종 상태 동일해야 함

**리팩터링 시 금지 사항:**
- 트랜잭션 경계 변경
- 예외 타입 변경
- 응답 포맷 변경
- 상태 전이 규칙 변경
- 동작 순서 변경으로 인한 부작용

**자유롭게 개선 가능한 영역:**
- 클래스 책임 분리, 메서드 추출, 도메인 객체 도입
- 서비스 분리, 의존성 방향 수정, 중복 제거, 네이밍 개선

### 보호 대상

보호 대상은 **최종 결과**이지 메서드 호출이 아니다.

- ✅ 최종 재고 수량, DB 상태, API 응답, 비즈니스 결과
- ❌ decrease() 호출 여부, 특정 Repository 호출 여부, 특정 클래스 존재 여부

## 인수 테스트 규칙

### 테스트 형식
- **Cucumber BDD + Gherkin** 형식으로 작성
- 한글 Given/When/Then 사용 (`io.cucumber.java.ko`)
- 기술 용어(HTTP, JSON, status code) 대신 **비즈니스 도메인 언어** 사용
- Feature 파일 위치: `src/test/resources/features/`

### 데이터 및 격리
- 테스트 데이터는 **Repository로 직접 삽입** (다른 API로 데이터 준비 금지)
- 각 시나리오마다 DatabaseCleaner로 **DB 초기화** (Cucumber `@Before` hook)
- `@Transactional` 자동 롤백에 의존하지 않는다 (서버와 테스트가 별도 스레드)
- Step Definition 간 상태 공유는 **ScenarioContext**(`@ScenarioScope`)로만 한다

### 의존성 격리
- 외부 의존(배송 등)은 **Fake/Stub**으로 격리
- 내부 컴포넌트에 `@MockBean` 사용 금지 — Mock하면 해당 구간이 검증에서 빠짐
- **DB 최종 상태 검증 우선** (`verify(mock)` 지양)

### 환경
- 테스트 DB는 **PostgreSQL** (Production Parity — H2 사용 금지)
- **Docker Compose**로 테스트 인프라 관리
- 애플리케이션은 **Docker 컨테이너**로 실행하여 E2E 테스트
- 단일 커맨드(`./gradlew cucumberTest`)로 전체 테스트 실행 가능해야 한다

### 테스트 전략
- **리스크 기반**으로 자동화 대상 선택: Risk = Impact × Likelihood × Detection Cost
- High Risk → Cucumber 자동화 / Medium → 선택적 / Low → 수동 QA

## 요구 사항

### 1. 선물 보내기

사용자가 상품을 선택하고 수량을 입력하여 선물을 요청하면:
- 선물 요청이 생성되어야 한다
- 재고가 해당 수량만큼 감소해야 한다
- 정상 응답이 반환되어야 한다
- 트랜잭션은 원자적으로 처리되어야 한다

### 2. 재고 부족

재고보다 많은 수량을 요청하면:
- 예외가 발생해야 한다
- 재고는 변경되지 않아야 한다
- 선물 요청은 생성되지 않아야 한다

### 3. 잔액 부족

잔액이 부족한 상태에서 결제를 시도하면:
- 결제가 실패해야 한다
- 잔액은 차감되지 않아야 한다
- 선물은 생성되지 않아야 한다

### 4. 상태 전이 규칙

- 생성된 선물은 취소 가능하다
- 취소 시 재고는 복구되어야 한다
- 잘못된 상태 전이는 허용되지 않는다

### 5. 시스템 경계 기준 보호

테스트에서 반드시 검증해야 할 항목:
- HTTP 상태 코드, 응답 Body
- DB 최종 상태, 재고 수량, 잔액 수치, 선물 상태 값

내부 메서드 호출 여부는 보호 대상이 아니다.

## 상세 참고자료

- 프로젝트 구조/API/엔티티 상세 분석: `.claude/docs/project-analysis.md`
- 1단계 테스트 시나리오 상세 설계: `.claude/docs/test-strategy.md`
- 2단계 고도화 가이드 (Cucumber, PostgreSQL, Docker): `.claude/docs/acceptance-testing-guide.md`
