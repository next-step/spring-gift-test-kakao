# spring-gift-test

## Cucumber BDD 테스트 실행

### 전체 테스트 실행 (Cucumber + JUnit)
```bash
./gradlew test
```

### Cucumber 테스트만 실행
```bash
./gradlew test --tests "gift.CucumberTest"
```

### Feature 파일 위치
```
src/test/resources/features/
├── category.feature   # 카테고리 관리
├── product.feature    # 상품 관리
└── gift.feature       # 선물하기
```