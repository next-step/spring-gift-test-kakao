#noinspection NonAsciiCharacters
Feature: 선물하기
  사용자가 다른 사용자에게 선물을 보낼 수 있다.

  Background:
    Given 회원 "철수"가 등록되어 있다.
    And 회원 "영희"가 등록되어 있다.
    And 카테고리 "필기구"가 등록되어 있다.
    And 상품 "연필"가 등록되어 있다.
    And 상품의 옵션 "연필 옵션"이 등록되어 있다.

  Scenario: 사용자가 선물을 보낸다
    When "철수"가 "영희"에게 옵션 "연필 옵션" 100개를 선물한다
    Then 상태 코드는 200이다

  Scenario: 선물 요청 시 옵션 id가 제공되지 않으면 에러가 발생한다
    When "철수"가 "영희"에게 옵션 id 없이 선물 보내기를 요청한다
    Then 상태 코드는 400이다

  Scenario: 선물 요청 시 존재하지 않는 옵션 id가 제공되면 에러가 발생한다
    When "철수"가 "영희"에게 존재하지 않는 옵션 id로 선물 보내기를 요청한다
    Then 상태 코드는 404이다

  Scenario: 선물 요청 시 수신자 id가 제공되지 않으면 에러가 발생한다
    When 수신자 id 없이 "철수"가 "축하해" 메세지로 옵션 "연필 옵션"를 재고 5 만큼 선물 보내기를 요청한다
    Then 상태 코드는 400이다

  Scenario: 선물 요청 시 존재하지 않는 수신자 id가 제공되면 에러가 발생한다
    When 존재하지 않는 수신자 id로 "철수"가 "축하해" 메세지로 옵션 "연필 옵션"를 재고 5 만큼 선물 보내기를 요청한다
    Then 상태 코드는 404이다

  Scenario: 선물 요청 시 Member-Id 헤더가 누락되면 에러가 발생한다
    When Member-Id 헤더 없이 "영희"에게 "축하해" 메세지로 옵션 "연필 옵션"을 5 만큼 선물 보내기를 요청한다
    Then 상태 코드는 400이다

  Scenario: 선물 요청 시 존재하지 않는 Member-Id가 제공되면 에러가 발생한다
    When 존재하지 않는 Member-Id로 "영희"에게 "축하해" 메세지로 옵션 "연필 옵션"을 5 만큼 선물 보내기를 요청한다
    Then 상태 코드는 404이다

  Scenario Outline: 재고가 <stock>개일 때 <quantity>개를 선물하면 <result>
    Given 상품의 옵션 "경계값 옵션"이 재고 <stock>으로 등록되어 있다.
    When "철수"가 "영희"에게 옵션 "경계값 옵션" <quantity>개를 선물한다
    Then 상태 코드는 <statusCode>이다

    Examples:
      | stock | quantity | statusCode | result |
      | 10    | 9        | 200        | 성공한다   |
      | 10    | 10       | 200        | 성공한다   |
      | 10    | 11       | 500        | 실패한다   |
      | 0     | 1        | 500        | 실패한다   |
