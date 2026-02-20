-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 조회 대상 카테고리 3건
INSERT INTO category (id, name) VALUES (1, '전자기기');
INSERT INTO category (id, name) VALUES (2, '의류');
INSERT INTO category (id, name) VALUES (3, '식품');
