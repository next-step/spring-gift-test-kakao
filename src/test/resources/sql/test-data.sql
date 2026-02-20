SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 회원 (API 없으므로 직접 삽입)
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');

-- 카테고리
INSERT INTO category (id, name) VALUES (100, '테스트카테고리');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id)
VALUES (100, '테스트상품', 10000, 'http://test.com/image.jpg', 100);

-- 옵션 (API 없으므로 직접 삽입)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '옵션A', 10, 100);
INSERT INTO option (id, name, quantity, product_id) VALUES (2, '옵션B', 1, 100);
