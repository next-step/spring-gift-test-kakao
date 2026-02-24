# 선물하기 서비스 - Cucumber BDD 테스트 가이드

기존 RestAssured + JUnit5 기반 인수 테스트를 **Cucumber BDD** 형식으로 전환하고,
**PostgreSQL + Docker Compose**로 테스트 인프라를 구성하여 실제 운영 환경에 가까운 테스트를 수행한다.

---

## 현재 상태 (1단계 완료)

- Cucumber BDD 테스트가 **H2 In-Memory DB** 위에서 동작 중
- Feature 파일: `src/test/resources/features/` (category, product, gift)
- Step Definitions: `src/test/java/gift/cucumber/steps/`
- CucumberSpringConfiguration: H2 전용 TRUNCATE 문법 사용 중 (`SET REFERENTIAL_INTEGRITY`)
- 기존 AcceptanceTest: H2 + `@Sql` 어노테이션으로 동작 중

### 2단계 목표

Cucumber 테스트의 DB를 **H2 → PostgreSQL(Docker Compose)** 로 전환한다.
기존 AcceptanceTest는 H2를 그대로 사용한다.

---

## 핵심 원칙

1. **한글 Gherkin 시나리오:** `조건/만일/그러면` 한글 키워드로 비즈니스 행동을 묘사한다.
2. **상태 변화 검증:** 실패 시나리오를 통해 재고 부족 등 상태 변화를 증명한다.
3. **시나리오 간 데이터 격리:** Cucumber `@Before` 훅에서 DB를 TRUNCATE하여 테스트 간 간섭을 방지한다.
4. **Step 재사용:** Given/When/Then 스텝을 재사용 가능하게 작성한다.
5. **DB 분리:** Cucumber 테스트는 PostgreSQL(Docker Compose), 기존 AcceptanceTest는 H2를 사용한다.

---

## 기술 스택

| 구성 요소 | 기술 |
| --- | --- |
| BDD 프레임워크 | Cucumber 7.x (`cucumber-java`, `cucumber-spring`) |
| 한글 스텝 | `io.cucumber.java.ko` 패키지 |
| HTTP 테스트 | RestAssured |
| Spring 통합 | `@CucumberContextConfiguration` + `@SpringBootTest` |
| DB (Cucumber) | PostgreSQL 16 (Docker Compose, 포트 `15432`) |
| DB (AcceptanceTest) | H2 In-Memory |
| 테스트 러너 | JUnit Platform Suite API |

---

## 테스트 실행

```bash
./gradlew cucumberTest    # Cucumber만 (PostgreSQL 자동 시작/종료)
./gradlew test            # AcceptanceTest만 (H2, Cucumber 제외)
```

---

## 테스트 분리 구조

- **기본 `test` 태스크:** `gift/cucumber/**` 패턴을 제외하여 AcceptanceTest만 실행한다.
- **`cucumberTest` 태스크:** `gift/cucumber/**` 패턴만 포함하여 Cucumber 테스트만 실행한다.
- CucumberSpringConfiguration에 `@ActiveProfiles("cucumber")`를 선언하여 PostgreSQL 프로파일을 활성화한다.

---

## 변경 금지 사항

- 기존 `application.properties` 수정 금지
- 기존 `*AcceptanceTest` 파일 수정 금지
- H2 의존성(`com.h2database:h2`) 제거 금지
- Entity 클래스 수정 금지
- Feature 파일, Step Definitions 수정 금지 (PostgreSQL 전환 시)

---

## 참고

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- [Gradle Exec Task](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html)
- [Cucumber 공식 문서](https://cucumber.io/docs/cucumber/)
