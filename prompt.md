# AI 활용 프롬프트 기록

## 1. 선물하기 테스트 재고 차감 검증 방식 변경

- **프롬프트**: 선물하기 테스트에서 db 상태 검사는 지우려고 함. 대신에 기존 재고를 다 소진하는 선물하기를 호출 하고 다신 한번 선물 하기를 호출해서 실패하는지를 확인해보는 방식으로 재고 차감을 테스트 해보려고 해. 이렇게 바꿔줘
- **AI 결과**: `선물을_보낸다` 테스트에서 DB 조회(`optionRepository.findById` + `getQuantity()`)를 제거하고, 재고 전량(10개)을 선물한 뒤 1개를 추가 선물하여 실패(500)하는지 확인하는 방식으로 변경
- **선택/폐기**: 재고 차감 검증 로직은 채택했으나, 성공 케이스가 삭제된 문제를 발견하여 추가 요청

## 2. 선물하기 성공 케이스 복원

- **프롬프트**: 선물하기 성공하는 케이스가 삭제된 것 같음
- **AI 결과**: 기존 성공 테스트(`선물을_보낸다`)를 DB 검증 없이 200 OK만 확인하는 형태로 별도 복원
- **선택/폐기**: 채택. 최종적으로 테스트 4개 구성 (성공, 재고 차감, 존재하지 않는 옵션, 재고 초과)

## 3. Cucumber 인수테스트 예제 코드 요청

- **프롬프트**: cucumber 로 인수테스트를 만드려고 하는데 실제로 만들기 전에 간단한 예제 코드를 만들어서 보여줘.
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택. 실제 적용 진행

## 4. Cucumber 인수테스트 실제 적용

- **프롬프트**: 실제 적용 해봐
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 5. 기존 테스트 전체를 Cucumber 버전으로 작성

- **프롬프트**: 기존의 테스트 코드들 CategoryAcceptanceTest, GiftAcceptanceTest, ProductAcceptanceTest 는 그대로 놔두지만 이 테스트 케이스들과 테스트 코드를 참고해서 각각의 파일로 cucumber 버전 코드를 만들어봐
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 6. PostgreSQL 연결 방법 질문

- **프롬프트**: 이제 h2 말고 postgres 에 연결하도록 하고 싶은데 어떻게 하면 좋을까
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택. Docker로 직접 PostgreSQL 띄우고 application.properties에 연결 설정

## 7. PostgreSQL 연결 설정 적용

- **프롬프트**: 도커로 내가 띄어 둘거니까 그냥 url, db, password 만 주면 연결할 수 있게 application.yml 파일 수정 해줘
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 8. 프로필 분리 (prod / cucumber)

- **프롬프트**: prod 랑 test 랑 프로필 분리하려고 함. prod 때는 ddl auto 끌거임. test 는 킬거고, 프로필 이름은 prod, cucumber 로 할거고, property 파일도 분리해줘.
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 9. Docker Compose + Gradle 태스크로 테스트 자동화

- **프롬프트**: docker-compose 로 postgreSQL 을 띄울 수 있게 해야됨. gradle task 를 추가해서 gradle 명령어로 docker-compose 실행되게 만들어야됨. pg_isready 로 테스트 실행 전에 헬스 체크 성공하면 테스트 돌아가야하고, 실패하면 5번정도 retry. doFirst / finalizedBy 사용해서 테스트 시작전 db 초기화.
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 10. Dockerfile multi-stage build + docker-compose 통합 + .env 환경변수

- **프롬프트**: 스프링 application 까지 도커로 감싸야해. dockerfile 작성. multi-stage build (1단계 빌드, 2단계 실행). jvm은 eclipse-temurin:21-jre-alpine. 테스트 흐름: postgresql 실행 → healthcheck → 스프링 실행 → 헬스체크 → 테스트 실행. datasource url 등은 .env 파일로 주입.
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 11. Cucumber 시나리오 문구를 자연어로 변경

- **프롬프트**: cucumber 시나리오에서 then 에서 상태코드로 써있는데, 상태 코드는 개발에 따라 바뀔 수 있으니까 gherkin 문법 상에는 성공한다. 실패한다. 선물한다. 이런식으로 문구를 바꾸면 어떨까?
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 12. cucumber 프로필에서 환경변수 제거, prod만 환경변수 주입

- **프롬프트**: 환경변수를 테스트 할때는 어차피 docker-compose 로 띄어진 postgres 에 접근하니까 굳이 안넣어도 될 것 같음. prod 에 만 주입 시키면 될 것 같아서 수정해줘
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 13. test와 cucumberTest 태스크 분리

- **프롬프트**: ./gradlew test 에서는 도커 없이 h2 만으로 동작하고, ./gradlew cucumberTest 에서는 기존 처럼 도커 환경에서 돌아가도록 바꿔줘
- **AI 결과**: (아래 참고)
- **선택/폐기**: 채택

## 14. Cucumber 테스트를 Docker 앱에 요청하도록 변경

- **프롬프트**: 지금 도커로 스프링 어플리케이션을 띄우고 있잖아 그래서 cucumber 할 때 로컬 @SpringBootTest 로 띄우지 말고 도커로 띄어진 스프링 어플리케이션에 요청을 쏘면 좋겠어
- **AI 결과**: (아래 참고)
- **선택/폐기**: 미정
