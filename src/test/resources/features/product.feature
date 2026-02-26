Feature: 상품 API

  Scenario: 상품 생성 성공
    Given 이름이 "전자기기"인 카테고리가 등록되어 있다
    When 카테고리 "전자기기"에 이름 "노트북" 가격 1500000 이미지 "https://example.com/notebook.png"인 상품을 생성하면
    Then 응답 상태 코드는 200이다
    And 응답의 "id" 필드는 null이 아니다
    And 응답의 "name" 필드는 "노트북"이다
    And 응답의 정수 필드 "price"는 1500000이다
    And 응답의 "imageUrl" 필드는 "https://example.com/notebook.png"이다
    And 응답의 "category.name" 필드는 "전자기기"이다

  Scenario: 상품 생성 실패 - 존재하지 않는 카테고리
    When 존재하지 않는 카테고리로 상품을 생성하면
    Then 응답 상태 코드는 500이다

  Scenario: 상품 목록 조회 - 빈 목록
    When 상품 목록을 조회하면
    Then 응답 상태 코드는 200이다
    And 응답 목록의 크기는 0이다

  Scenario: 상품 목록 조회 - N개 존재
    Given 이름이 "전자기기"인 카테고리가 등록되어 있다
    And 카테고리 "전자기기"에 이름 "노트북" 가격 1500000 이미지 "https://example.com/notebook.png"인 상품이 등록되어 있다
    And 카테고리 "전자기기"에 이름 "키보드" 가격 120000 이미지 "https://example.com/keyboard.png"인 상품이 등록되어 있다
    When 상품 목록을 조회하면
    Then 응답 상태 코드는 200이다
    And 응답 목록의 크기는 2이다
    And 응답 목록의 "name"에 "노트북", "키보드"이 순서 무관하게 포함되어 있다
    And 응답 목록의 정수 "price"에 1500000, 120000이 순서 무관하게 포함되어 있다
    And 응답 목록의 모든 "category.name"은 "전자기기"이다
