Feature: 상품 관리
  사용자가 상품을 추가하고 조회할 수 있다.

  Background:
    Given 카테고리 "과일"가 등록되어 있다.

  Scenario: 사용자가 상품을 추가한다
    When 상품명 "사과", 가격 100, 이미지 "https://apple.com"으로 상품을 추가한다
    Then 상태 코드는 200이다
    And 응답의 "id"는 비어있지 않다
    And 응답의 "name"은 "사과"이다
    And 응답의 정수 "price"는 100이다
    And 응답의 "imageUrl"은 "https://apple.com"이다

  Scenario: 사용자가 상품을 조회한다
    Given 상품 "바나나"가 가격 1000, 이미지 "https://banana.com"으로 등록되어 있다.
    When 상품 목록을 조회한다
    Then 상태 코드는 200이다
    And 응답 목록의 크기는 1이다
    And 응답 목록의 "name"에 "바나나"가 포함되어 있다

  Scenario: 사용자가 상품을 추가하고 조회한다
    When 상품명 "배", 가격 100, 이미지 "https://pear.com"으로 상품을 추가한다
    Then 상태 코드는 200이다
    When 상품 목록을 조회한다
    Then 응답 목록의 크기는 1 이상이다
    And 응답 목록의 "name"에 "배"가 포함되어 있다

  Scenario: 상품이 없으면 빈 목록이 조회된다
    When 상품 목록을 조회한다
    Then 상태 코드는 200이다
    And 응답 목록의 크기는 0이다

  Scenario: 상품 추가 시 카테고리 id가 제공되지 않으면 에러가 발생한다
    When 카테고리 id 없이 상품 추가를 요청한다
    Then 상태 코드는 400이다

  Scenario: 상품 추가 시 존재하지 않는 카테고리 id가 제공되면 에러가 발생한다
    When 존재하지 않는 카테고리 id로 상품 추가를 요청한다
    Then 상태 코드는 404이다

  Scenario: 상품 가격이 음수면 에러가 발생한다
    When 가격 -10으로 상품 추가를 요청한다
    Then 상태 코드는 400이다
