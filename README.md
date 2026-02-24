# spring-gift-test

## 테스트 실행

### AcceptanceTest (H2, Docker 불필요)

```bash
./gradlew test
```

### Cucumber 테스트 (Docker 환경)

```bash
./gradlew dockerBuild         # Docker 이미지 빌드
./gradlew dockerUp            # App + DB 컨테이너 시작
./gradlew cucumberTest        # Cucumber 테스트 실행
./gradlew dockerDown          # 컨테이너 종료
```