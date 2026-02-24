# Cucumber 인수 테스트 독립 실행 환경 구성

## 개요

- 실행 속도가 빠른 단위 테스트(test)와 실행 속도가 느린 Cucumber 인수 테스트(cucumberTest)를 물리적으로 분리
- 개발 중에는 단위 테스트만 빠르게 수행하고, 배포 전이나 필요할 때 인수 테스트 별도 수행가능하도록 구성

## Gradle 코드 분석

### configurations

```Groovy
configurations {
    cucumberRuntime {
        extendsFrom testImplementation
    }
}
```

- configurations: Gradle에서 라이브러리 그룹화 단위
- cucumberRuntime: cucumberRuntime 그룹 정의
- extendsFrom testImplementation: 새로 만든 그룹이 기존 testImplementation에 있는 모든 라이브러리 상속받게 함

### Task Registration

```Groovy
tasks.register('cucumberTest', Test) {
    // 설정 내용
}
```

- tasks.register('cucumberTest', Test): 타입이 Test인 cucumberTest 작업 생성. 
  - Gradle 표준 테스트 기능 사용 (리포트 생성, 결과 캐싱 등)

#### 세부 설정 내용
1. 메타데이터
   - description: ./gradlew tasks 명령어 수행 시 보이는 설명
   - group = 'verification': 이 태스크를 verification 그룹에 묶는다.

2. 클래스패스 설정
    - testClassesDirs: 테스트 코드가 컴파일된 폴더 위치 지정
    - classpath: 테스트 실행에 필요한 라이브러리 경로 지정

3. JUnit 플랫폼 & 태그 필터링

```Groovy
useJUnitPlatform {
    includeEngines 'cucumber'
}
```

- .feature 파일들의 시나리오를 모두 실행한다.

4. 실행 순서
- shouldRunAfter test: ./gradlew build 처럼 전체를 빌드할 때, 기본 단위 테스트가 다 끝나고 성공한 뒤에 cucumberTest 실행

### 장점

- ./gradlew test, ./gradlew cucumberTest로 테스트 구분
    - 역할 분리
    - CI/CD 단계 분리 가능
