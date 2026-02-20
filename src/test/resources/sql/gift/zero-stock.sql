-- 시나리오: 재고가 0인 옵션에 선물 요청
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 10000, 'img.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 0, 1);
