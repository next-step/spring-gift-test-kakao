Feature: 상품 관리

  Scenario: 유효한 상품을 등록한다
    Given 이름이 "교환권"인 카테고리가 등록되어 있고
    When "스타벅스 아메리카노" 상품을 가격 4500, 이미지 "https://example.com/coffee.jpg"로 등록하면
    Then 응답 상태 코드는 200이다
    And 응답의 id는 비어있지 않다
    And 응답의 상품명은 "스타벅스 아메리카노"이다
    And 응답의 가격은 4500이다
    And 응답의 이미지는 "https://example.com/coffee.jpg"이다
    And 응답의 카테고리명은 "교환권"이다

  Scenario: 상품 목록을 조회한다
    Given 이름이 "교환권"인 카테고리가 등록되어 있고
    And "스타벅스 아메리카노" 상품을 가격 4500, 이미지 "https://example.com/americano.jpg"로 등록되어 있고
    And "스타벅스 카페라떼" 상품을 가격 5000, 이미지 "https://example.com/latte.jpg"로 등록되어 있고
    When 상품 목록을 조회하면
    Then 응답 상태 코드는 200이다
    And 응답 목록의 크기는 2이다
    And 응답 목록에 상품명 "스타벅스 아메리카노"가 포함되어 있다
    And 응답 목록에 상품명 "스타벅스 카페라떼"가 포함되어 있다
    And 응답 목록의 첫 번째 상품에 카테고리 정보가 포함되어 있다
