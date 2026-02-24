# Cucumber 테스트 자동화 - Docker & PostgreSQL

## Gradle 태스크 자동화

- 테스트 실행 시 Docker 컨테이너 자동으로 관리(Up/Down)하고, cucumber 프로필을 강제.

```Groovy
task dockerComposeUp(type: Exec) {
    group = 'docker'
    description = 'Docker Compose를 백그라운드로 실행'
    commandLine 'docker-compose', 'up', '-d'
}

task dockerComposeDown(type: Exec) {
    group = 'docker'
    description = 'Docker Compose 종료 및 컨테이너 삭제'
    commandLine 'docker-compose', 'down'
}

// Cucumber 테스트 전용
task cucumberTest(type: Test) {
    group = 'verification'
    description = 'Docker DB 띄우고 Cucumber 인수 테스트 진행'
    
    // 테스트 실행 전 docker-compose up
    dependsOn dockerComposeUp
    
    // 테스트 성공/실패 상관없이 종료 후 Docker 내리기
    finalizedBy dockerComposeDown
    
    // JUnitPlatform 사용
    useJUnitPlatform()
    
    // 시스템 프로퍼티 설정
    systemProperty "spring.profiles.active", "cucumber"
    
    // test 로그 출력 설정
    testLogging {
        events "passed", "skipped", "failed"
    }
}
```

## 테스트 환경 설정

- 테스트 실행 시 application-cucumber.properties 설정 읽어오도록 프로필 명시.
    - @ActiveProfiles("cucumber")

## 데이터베이스 초기화 SQL 변경

- H2와 달리 PostgreSQL에서는 FK 제약 조건이 상대적으로 엄격해서  TRUNCATE ... CASCADE 문법을 사용해야 한다.

## 테스트 분리 - excludeEngines 적용

- test 태스크에서 cucumber 테스트 제외

```Groovy
tasks.named('test') {
  useJUnitPlatform {
    excludeEngines 'cucumber'
  }
}
```
