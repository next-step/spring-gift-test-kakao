-- 시나리오: 재고 충분, 정상 선물 보내기
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 10000, 'img.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 10, 1);
