# spring-gift-test

Spring Boot 기반 선물/상품 관리 시스템.

## 요구사항

- Java 21
- Docker

## 실행

Docker Compose로 PostgreSQL + 앱을 함께 실행합니다.

```bash
./gradlew dockerBuild   # Docker 이미지 빌드
./gradlew dockerUp      # 컨테이너 시작 (postgres + app)
```

앱은 `http://localhost:28080`에서 접근 가능합니다.

```bash
./gradlew dockerDown    # 컨테이너 종료
```

## 테스트

컨테이너가 실행 중인 상태에서 Cucumber 인수 테스트를 실행합니다.

```bash
./gradlew dockerUp        # 컨테이너 시작
./gradlew cucumberTest    # 테스트 실행
./gradlew dockerDown      # 컨테이너 종료
```
