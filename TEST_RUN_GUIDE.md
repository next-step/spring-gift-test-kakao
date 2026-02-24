# Test 실행 가이드

## 개요
현재 Gradle 설정 기준으로 테스트 태스크는 아래처럼 분리되어 있습니다.

- `./gradlew test`: Cucumber BDD 테스트만 실행
- `./gradlew cucumberTest`: Docker(App + PostgreSQL) 기반 Cucumber 테스트 실행
- `./gradlew step1Test`: 기존 RestAssured 인수 테스트(1단계)만 실행
- `./gradlew dockerBuild/dockerUp/dockerDown`: 애플리케이션 컨테이너 실행/종료

## 1) Cucumber 테스트 실행
명령어:

```bash
./gradlew test
```

실행 대상:

- `gift.cucumber.CucumberTest`
- `src/test/resources/features/*.feature` 시나리오
- step definitions: `src/test/java/gift/cucumber/steps/*.java`

설정 근거:

- `build.gradle`의 `test` 태스크 필터가 `gift.cucumber.CucumberTest`로 제한되어 있음

## 2) Docker(App + PostgreSQL) 기반 Cucumber 실행
명령어:

```bash
./gradlew cucumberTest
```

실행 흐름:

- `dockerComposeUp` 실행 (`docker compose up -d --wait`)
- `gift.cucumber.CucumberTest` 실행 (프로파일: `test`)
- 완료 후 `dockerComposeDown` 실행 (`docker compose down -v`)

설정 근거:

- `build.gradle`의 `cucumberTest` 태스크 (`dependsOn dockerComposeUp`, `finalizedBy dockerComposeDown`)
- `src/test/resources/application-test.properties`의 PostgreSQL 설정

## 3) 컨테이너 실행(요구사항 3)
명령어:

```bash
./gradlew dockerBuild
./gradlew dockerUp
curl http://localhost:28080/api/categories
./gradlew cucumberTest
./gradlew dockerDown
```

검증 포인트:

- `docker compose ps`에서 `app`, `postgres`가 `healthy`
- `curl http://localhost:28080/api/categories` 응답 확인
- `curl http://localhost:28080`가 404인 것은 정상(루트 매핑 없음)

## 4) 1단계 인수 테스트 실행
명령어:

```bash
./gradlew step1Test
```

실행 대상:

- `gift.CategoryAcceptanceTest`
- `gift.ProductAcceptanceTest`
- `gift.GiftAcceptanceTest`

설정 근거:

- `build.gradle`의 `step1Test` 태스크 `includeTestsMatching` 필터

## 5) 개별 테스트 클래스만 실행
특정 클래스만 실행할 때:

```bash
./gradlew test --tests gift.cucumber.CucumberTest
./gradlew step1Test --tests gift.GiftAcceptanceTest
```

## 주의사항
- `./gradlew test`는 기본 실행이며 Docker 없이 실행됩니다.
- `./gradlew cucumberTest`는 `dockerUp` 후 테스트를 실행하고 종료 시 `dockerDown`을 수행합니다.
- Cucumber 시나리오는 `DatabaseCleanUp` 훅으로 시나리오마다 DB를 초기화합니다.
