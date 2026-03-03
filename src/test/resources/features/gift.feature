Feature: 선물 보내기

  Background:
    Given 이름이 "식품"인 카테고리가 등록되어 있고
    And "아메리카노" 상품을 가격 4500, 이미지 "http://image.url"로 등록되어 있고
    And 재고 10개인 "ICE" 옵션이 등록되어 있고
    And 회원 "홍길동"이 존재할 때

  Scenario: 유효한 요청으로 선물을 보내면 200 OK와 재고가 차감된다
    When "홍길동"이 "ICE" 옵션 3개를 선물하면
    Then 응답 상태 코드는 200이다
    And "ICE" 옵션의 재고는 7이다
    And 선물 배달이 호출된다

  Scenario: 존재하지 않는 옵션으로 선물을 보내면 500 에러가 발생한다
    When "홍길동"이 존재하지 않는 옵션으로 선물하면
    Then 응답 상태 코드는 500이다

  Scenario: 재고보다 많은 수량을 요청하면 500 에러가 발생한다
    When "홍길동"이 "ICE" 옵션 15개를 선물하면
    Then 응답 상태 코드는 500이다
    And "ICE" 옵션의 재고는 10이다

  Scenario: Member-Id 헤더가 없으면 400 에러가 발생한다
    When Member-Id 없이 "ICE" 옵션 3개를 선물하면
    Then 응답 상태 코드는 400이다

  Scenario: 재고와 동일한 수량을 요청하면 200 OK와 재고가 0이 된다
    When "홍길동"이 "ICE" 옵션 10개를 선물하면
    Then 응답 상태 코드는 200이다
    And "ICE" 옵션의 재고는 0이다
