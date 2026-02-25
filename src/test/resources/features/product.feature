Feature: 상품관리 시스템
  상품 관리 시스템을 통해서 상품을 생성하고 조회한다.

  Background:
    Given "굿즈" 카테고리가 등록되어 있다.

  Scenario: 상품을 생성한다.
    When "춘식이 충전기"를 가격 20000원과 이미지 "https://kakao.com/choonsik_charge.jpg"로 생성한다.

    Then 상품의 이름은 "춘식이 충전기" 이다.
    And 상품의 가격은 20000원 이다.
    And 응답 상태 코드는 200을 반환한다.

  Scenario: 상품 목록을 조회한다.
    Given "춘식이 충전기"가 가격 20000원과 이미지 "https://kakao.com/choonsik_charge.jpg"로 등록되어있다.
    And "춘식이 선풍기"가 가격 5000원과 이미지 "https://kakao.com/choonsik_fan.jpg"로 등록되어있다.

    When 상품 목록을 조회한다.

    Then 선물 목록의 크기는 2이다.
    And 응답 상태 코드는 200을 반환한다.

  Scenario: 상품이 없으면 빈 목록을 반환한다.
    When 상품 목록을 조회한다.

    Then 선물 목록의 크기는 0이다.
    And 응답 상태 코드는 200을 반환한다.

  Scenario: 존재하지 않는 카테고리의 상품을 생성하면 실패한다.
    When 존재하지 않는 카테고리의 "춘식이 키링" 상품을 가격 8000원, 이미지 "https://kakao.com/choonsik_key.jpg"으로 생성한다.

    Then 응답 상태 코드는 500을 반환한다.

