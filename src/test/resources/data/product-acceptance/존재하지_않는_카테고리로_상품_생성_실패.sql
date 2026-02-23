-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 카테고리가 없는 빈 상태에서 시작 (INSERT 없음)
-- 존재하지 않는 categoryId로 상품 생성 시 실패를 검증
