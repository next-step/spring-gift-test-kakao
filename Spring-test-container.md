## ./gradlew test 호출 흐름:

1. Gradle test 태스크 실행
   → 클래스패스에서 테스트 클래스 탐색

2. gift/CucumberTest.class 발견
   → @Suite + @IncludeEngines("cucumber") → Cucumber 엔진 실행
   → @SelectClasspathResource("features") → .feature 파일 로드

3. Cucumber가 glue 경로 스캔

4. CucumberSpringConfig 처리
   → @SpringBootTest(RANDOM_PORT) → Spring 컨텍스트 생성 + 내장 Tomcat 기동
   → @Profile("cucumber-alone") 불일치 → Testcontainers 안 뜸

5. CommonSteps.setUp() 실행

6. 13개 시나리오 실행
   → RestAssured

## testContainer 가 docker 을 호출하는 방법

start()가 호출되면 Testcontainers가 Docker API를 통해 docker run postgres:16을 실행하는 것과 동일한 동작을 합니다.

Spring @Bean 호출
→ new PostgreSQLContainer<>("postgres:16")  ← Java 객체만 생성
→ start()                                    ← 여기서 Docker API 호출
→ docker pull postgres:16 (없으면)
→ docker run -p {랜덤포트}:5432 postgres:16
→ PostgreSQL ready 될 때까지 대기

Testcontainers는 docker CLI를 직접 호출하는 게 아니라 Docker 소켓(/var/run/docker.sock)에 HTTP 요청을 보내서 컨테이너를 관리합니다. 이때 내부적으로 `ryuk` 와 `socat` 을 동작시킵니다.

## ryuk
Testcontainers가 함께 띄우는 청소 담당 컨테이너입니다.

테스트가 끝나거나 JVM이 종료되면 Ryuk이 테스트에서 생성된 컨테이너들(postgres, socat 등)을
감지해서 자동으로 remove합니다.

비정상 종료(kill -9, 크래시 등)에도 Ryuk 자체는 Docker 데몬에 살아있어서 고아 컨테이너가 남지 않게
정리해줍니다. Ryuk 본인도 마지막에 스스로 내려갑니다.

### socat
포트 포워딩용으로 띄우는 컨테이너입니다.

ComposeContainer에서 withExposedService()를 쓰면, socat이 호스트와 컨테이너 네트워크 사이에서
트래픽을 중계합니다.

호스트 (Spring Boot) → socat → postgres 컨테이너 docker-compose로 띄운 컨테이너는 자체 네트워크에 있어서, 호스트에서 직접 접근이 어렵습니다. 따라서 socat이 그 사이를 연결해주는 역할입니다.

현재는 postgre 만 docker 로 띄운후 해당 db 와 application 이 동작하는 형태이다.
이때 postgre 는 docker network 안에만 있으므로 밖에서 직접 접근을 할 수 없어 socat를 활용합니다.
