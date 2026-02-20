-- 시나리오: 재고와 요청 수량이 정확히 일치 (경계값)
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 10000, 'img.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 5, 1);
