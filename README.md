# spring-gift-test

## 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew test
```

기존 RestAssured 인수 테스트와 Cucumber BDD 시나리오가 함께 실행됩니다.

### Cucumber 시나리오 구조
- 시나리오 정의: `src/test/resources/features/*.feature`
- Step 구현: `src/test/java/gift/acceptance/cucumber/`