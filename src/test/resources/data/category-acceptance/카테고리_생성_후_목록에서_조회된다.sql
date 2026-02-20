-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 카테고리 생성 후 목록 조회 테스트: 빈 상태에서 시작 (INSERT 없음)
