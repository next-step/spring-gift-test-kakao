-- 카테고리
INSERT INTO category (id, name) VALUES (1, '간식');
INSERT INTO category (id, name) VALUES (2, '음료');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 5000, 'http://img.com/choco.png', 1);
INSERT INTO product (id, name, price, image_url, category_id) VALUES (2, '커피', 3000, 'http://img.com/coffee.png', 2);

-- 옵션 (재고 관리 대상)
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '초콜릿 기본', 10, 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (2, '커피 기본', 1, 2);

-- 회원
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
