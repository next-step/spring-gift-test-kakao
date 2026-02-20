-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 카테고리
INSERT INTO category (id, name) VALUES (1, '전자기기');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '맥북 에어', 1500000, 'https://example.com/macbook.png', 1);

-- 옵션: 재고 충분 (10개)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, 'M2 Silver', 10, 1);

-- 보내는 사람
INSERT INTO member (id, name, email) VALUES (1, '보내는이', 'sender@kakao.com');

-- 받는 사람
INSERT INTO member (id, name, email) VALUES (2, '받는이', 'receiver@kakao.com');
