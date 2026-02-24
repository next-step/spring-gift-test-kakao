# spring-gift-test

카카오 선물하기 시스템의 인수 테스트 프로젝트입니다.

## 기술 스택

- Java 21, Spring Boot 3.5.8, Spring Data JPA
- Test: Cucumber 7 + JUnit 5, RestAssured
- DB: PostgreSQL (Docker Compose)
- Infra: Docker, Docker Compose

## 실행 방법

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### Cucumber 인수 테스트

PostgreSQL이 Docker Compose로 자동 시작되고, 테스트 완료 후 자동 종료됩니다.

```bash
./gradlew cucumberTest
```

### Docker 기반 E2E

애플리케이션까지 Docker 컨테이너로 실행하여 프로덕션과 동일한 환경에서 검증합니다.

```bash
./gradlew dockerBuild       # Docker 이미지 빌드
./gradlew dockerUp          # PostgreSQL + Application 컨테이너 시작
curl http://localhost:8080/api/categories  # 애플리케이션 응답 확인
./gradlew dockerDown        # 전체 컨테이너 종료
```
