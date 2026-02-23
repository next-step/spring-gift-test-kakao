@api @product
Feature: 관리자가 상품을 관리할 수 있다
  As a 관리자
  I want 상품을 생성하고 조회하고 싶다
  So that 상품을 체계적으로 관리할 수 있다

  Scenario: 상품이 없을 때 빈 목록 반환
    Given 상품이 존재하지 않는다
    When 관리자가 상품 목록을 조회한다
    Then 응답 상태 코드는 200이다
    And 상품 목록은 비어있다

  Scenario: 등록된 상품들이 목록에 포함
    Given "전자기기" 카테고리가 등록되어 있다
    And 다음 상품들이 등록되어 있다:
      | name | price   | imageUrl                         |
      | 노트북  | 1500000 | https://example.com/notebook.png |
      | 키보드  | 120000  | https://example.com/keyboard.png |
    When 관리자가 상품 목록을 조회한다
    Then 응답 상태 코드는 200이다
    And 상품 목록의 크기는 2이다
    And 상품 목록에 "노트북" 상품이 포함되어 있다
    And 상품 목록에 "키보드" 상품이 포함되어 있다

  Scenario: 새로운 상품을 생성하면 응답에 상품 정보가 포함된다
    Given "전자기기" 카테고리가 등록되어 있다
    When 관리자가 다음 상품을 생성한다:
      | name | price   | imageUrl                         |
      | 노트북  | 1500000 | https://example.com/notebook.png |
    Then 응답 상태 코드는 200이다
    And 응답에 생성된 상품 정보가 포함되어 있다:
      | name | price   | imageUrl                         | categoryName |
      | 노트북  | 1500000 | https://example.com/notebook.png | 전자기기         |
    And 데이터베이스에 "노트북" 상품이 저장되어 있다

  Scenario: 존재하지 않는 카테고리로 상품 생성 시 실패
    When 관리자가 존재하지 않는 카테고리로 상품을 생성한다:
      | name | price   | imageUrl                         | categoryId |
      | 노트북  | 1500000 | https://example.com/notebook.png | 9999       |
    Then 응답 상태 코드는 500이다
