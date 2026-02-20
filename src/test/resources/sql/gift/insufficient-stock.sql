-- 시나리오: 재고(2)보다 많은 수량(5) 요청
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 10000, 'img.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 2, 1);
