Feature: 선물 전달

  Scenario: 재고가 충분하면 선물 발송에 성공한다
    Given "ICE" 옵션의 재고가 10개 있다
    And 회원이 존재한다
    When 회원이 "ICE" 옵션 1개를 선물한다
    Then 선물 발송이 성공한다

  Scenario: 재고가 부족하면 선물 발송에 실패한다
    Given "ICE" 옵션의 재고가 1개 있다
    And 회원이 존재한다
    When 회원이 "ICE" 옵션 2개를 선물한다
    Then 재고 부족으로 실패한다

  Scenario: 존재하지 않는 옵션으로 선물하면 실패한다
    Given 회원이 존재한다
    When 존재하지 않는 옵션으로 선물한다
    Then 선물 발송이 실패한다
