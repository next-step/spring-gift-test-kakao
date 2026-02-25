INSERT INTO category (id, name) VALUES (1, '식품');
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '케이크', 30000, 'https://example.com/cake.jpg', 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 1, 1);
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com'), (2, '받는사람', 'receiver@test.com');
