
  # Claude Code(AI) 활용 기반 인수 테스트 작성 방법 정리

  본 문서는 **선물하기(Gift) 서비스 프로젝트**에서 Claude Code(또는 유사 AI 도구)를 활용해 **RestAssured 기반 인수 테스트(Acceptance Test)** 를 설계·작성한 과정과, 실제 사용한 **프롬프트/스킬 정의**를 정리한 문서입니다.
    
  ---

  ## 1. AI 활용 목적

    - 레거시 코드 구조와 유스케이스(행동) 파악
    - API 레벨 인수 테스트 시나리오 도출
    - RestAssured + JUnit5 기반 테스트 코드 생성
    - 성공 케이스뿐 아니라 실패 케이스를 포함하여 상태 변화 검증
    - 리팩토링 이후에도 깨지지 않도록 API 경계 중심으로 검증
    - 테스트 데이터(Seed) 구성 방식 결정 및 적용 방향 정리

    ---

  ## 2. 프로젝트 기능 분석에 사용한 프롬프트(Q)

  ### Q. 프로젝트 파악

    ```markdown
    ### 프로젝트 파악
    
    - **Q. 하단의 지침을 활용해 답변하시오**
        1. **당신은 레거시 코드에서 행동 테스트 코드를 짜기 위해 프로젝트를 분석해주는 전문가입니다.**
        2. **사용자가 할 수 있는 행동들을 파악합니다.**
        3. **어떤 행위가 주요 기능일지 파악합니다.**
        4. **작은 단위이면서 주요 기능인 것부터 나열해줍니다.**
    ```

  ### 결과로 정리된 기능 우선순위(요약)

    1. `Option.decrease()` (재고 차감 핵심 로직)
    2. `CategoryService.create()`
    3. `ProductService.create()`
    4. `OptionService.create()`
    5. `WishService.create()`
    6. `GiftService.give()` (핵심 플로우)
    7. `GiftRestController` API

    ---

  ## 3. 테스트 데이터 구성 방식 비교 프롬프트(Q)

  ### Q. 테스트 데이터셋 방식 장단점

    ```markdown
    Q. 테스트 코드를 위한 테스트 데이터셋을 SQL 스크립트를 파일로 작성하여 불러와서 실행하는 방식과
    로컬 DB 혹은 인메모리DB를 띄워서 테스트 데이터셋을 저장하는 방식의 장단점을 알고 싶어
    ```

  ### 결정 사항(요약)

    - 단위 테스트: 인메모리/Mock 또는 순수 단위 테스트 중심
    - 통합 테스트: 실제 DB 엔진(또는 컨테이너) 고려
    - 인수 테스트: 고정 Seed(SQL) 기반 초기화 전략 채택

    ---

  ## 4. claude.md(테스트 지침서) 작성 프롬프트(Q)

  ### Q. [과제] 클로드.md 작성

    ```markdown
    [과제] 클로드.md 작성 ( 하단의 지침들을 활용하여 )
    ```

  ### claude.md에서 고정한 규칙(요약)

    - BDD 도구(Cucumber, Karate 등) 사용하지 않음
    - RestAssured + JUnit5 사용
    - 실패 시나리오 포함(상태 변화 검증)
    - API 경계에서 검증
    - 고정 Seed 데이터(SQL) 활용

    ---

  ## 5. Acceptance Test Writer 스킬 설계/정의

  ### Q. 스킬 생성 요청

    ```markdown
    Acceptance Test Writer 라는 이름의 클로드 스킬을 만들려고해 아래 내용을 바탕으로 스킬을 만들어줘
    ```

  ### 스킬에 강제한 조건(요약)

    - `SpringBootTest(WebEnvironment.RANDOM_PORT)`
    - RestAssured 사용
    - `@Sql` 기반 데이터 초기화(고정 ID)
    - 최소 1개 성공 + 1개 실패(상태 변화) 포함
    - 출력은 Java 테스트 클래스 코드만(설명/마크다운 출력 금지)

    ---

  ## 6. 스킬 요구사항 변경(Seed 자동 생성 포함) 프롬프트(Q)

  ### Q. seed.sql이 없는 상황에서의 요구

    ```markdown
    seed.sql 없는데, 내가 원하는 스킬은 우리가 어떤 시나리오를 말해주면
    현재 프로젝트에서 해당하는 시나리오의 코드를 찾아서 파악한 후
    해당 시나리오를 위한 테스트 코드 작성 및 테스트 데이터셋도 한번에만들어주는 스킬을 만들고 싶었어
    ```

  ### 변경된 목표(요약)

    - 시나리오 입력 → 관련 코드 맥락 파악 → 테스트 코드 생성
    - 테스트에 필요한 데이터셋(Seed SQL)까지 함께 생성(또는 구성안 제시)
    - 실패 시나리오(상태 변화) 자동 포함

    ---

  ## 7. 실제 스킬 사용 기록(테스트 작성)

  ### 7.1 Gift API 인수 테스트

  ### Q. GiftRestController(POST /api/gifts) 행동 테스트 생성 요청

    ```markdown
    /acceptance-test-writer GiftRestController의 (POST /api/gifts) : API 레벨 행동 테스트 작성.
    /spring-gift-test-kakao/src/main/java/gift/application/GiftService.java 의 give의 맥락을 파악하고,
    요청에 대한 응답을 잘 해냈는지 테스트 코드를 작성하여, 기존의 코드가 리팩토링 되더라도 문제없이 테스트가 진행되로록 작성.
    ```

  ### AI가 제안한 시나리오 목록

    ```markdown
    void 선물하기_성공()
    void 선물하기_성공_후_재고가_차감되어_동일_요청이_실패한다()
    void 존재하지_않는_옵션으로_선물하기_실패()
    ```

  ### Q. 누락된 실패 케이스 추가 요청

    ```markdown
    보낸 사람이 존재하지 않은 사람에 대한 실패 테스트가 빠졌어
    ```

  ### 추가된 시나리오

    ```markdown
    void 존재하지_않는_보내는_사람으로_선물하기_실패()
    ```
    
  ---

  ### 7.2 Category API 인수 테스트

  ### Q. CategoryRestController(POST /api/categories) 테스트 요청

    ```markdown
    /acceptance-test-writer /gift/ui/CategoryRestController.java에 있는
    /api/categories POST 요청에 대한 인수 테스트 시나리오를 뽑고 인수 테스트 코드 작성해줘
    ```

    - 진행 중 확인된 사항: `@RequestBody` 애너테이션 누락(데이터 바인딩 이슈 가능)

  ### Q. 기존 코드 수정 없이 테스트만 작성 요청

    ```markdown
    기존 코드 수정은 추후에 할 예정이기 때문에, 지금은 기존 코드 수정 없이 오롯이 테스트 코드만 작성해야돼
    ```

  ### 생성된 시나리오

    ```markdown
    void 카테고리_생성_성공()
    void 카테고리_생성_후_목록에서_조회된다()
    ```
    
  ---

  ### 7.3 Category retrieve() 인수 테스트

  ### Q. retrieve() 함수 인수 테스트 요청

    ```markdown
    /acceptance-test-writer /gift/ui/CategoryRestController.java에서 retrieve()함수에 대한 인수 테스트 작성해줘
    ```

  ### 생성된 시나리오

    ```markdown
    void 카테고리_여러건_조회_성공()
    void 카테고리가_없으면_빈_배열이_반환된다()
    ```
    
  ---

  ### 7.4 Product API 테스트 시도 및 실패 판단

  ### Q. ProductRestController.create 인수 테스트 요청

    ```markdown
    /acceptance-test-writer API 레벨 행동 테스트 작성.
    spring-gift-test-kakao/src/main/java/gift/ui/ProductRestController.java 의 create의 맥락을 파악하고,
    요청에 대한 응답을 잘 해냈는지 테스트 코드를 작성하여, 기존의 코드가 리팩토링 되더라도 문제없이 테스트가 진행되로록 작성.
    기존 코드는 수정하지 않고, 테스트 코드만 작성
    ```

    - 결과: 데이터 바인딩 문제로 API 행동 테스트 작성이 어렵다고 판단(기존 코드 수정 없이는 안정적인 테스트 불가)

    ---

  ## 8. 최종 정리: 사용 방식(프로세스)

    1. 대상 API/컨트롤러와 시나리오를 프롬프트로 전달
    2. AI가 관련 코드 맥락을 기준으로 테스트 시나리오를 제안
    3. 누락된 실패 케이스는 추가 프롬프트로 보완
    4. 인수 테스트는 API 경계에서 상태 변화(성공 후 실패 등)로 검증
    5. 데이터 바인딩 등 구조적 문제 발견 시 “테스트 불가” 판단을 기록하고, 코드 수정 필요 여부를 분리 관리

    ---

  ## 9. 고정한 테스트 작성 원칙

    - RestAssured로 요청/응답을 검증한다.
    - 성공 케이스만 검증하지 않고 실패 케이스를 포함한다.
    - 상태 변화(예: 재고 감소 이후 재요청 실패)로 검증한다.
    - API 경계 기준으로 검증하고, 구현 상세에 의존하지 않는다.
    - 기존 코드 수정 없이 작성 가능한 범위에서 테스트를 우선 작성한다.
    - 테스트 작성이 불가능한 경우(예: 바인딩 문제)는 불가 사유를 기록한다.
