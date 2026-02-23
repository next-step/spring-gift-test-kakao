-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 조회 대상 카테고리
INSERT INTO category (id, name) VALUES (1, '전자기기');
INSERT INTO category (id, name) VALUES (2, '의류');

-- 조회 대상 상품 3건 (카테고리와 연관)
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '맥북 에어', 1500000, 'https://example.com/macbook.png', 1);
INSERT INTO product (id, name, price, image_url, category_id) VALUES (2, '아이패드', 800000, 'https://example.com/ipad.png', 1);
INSERT INTO product (id, name, price, image_url, category_id) VALUES (3, '나이키 후드티', 120000, 'https://example.com/hoodie.png', 2);
