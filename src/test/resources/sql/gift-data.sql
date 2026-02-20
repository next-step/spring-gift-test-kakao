INSERT INTO category (id, name) VALUES (1, '식품');
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '떡볶이', 5000, 'http://example.com/image.png', 1);
INSERT INTO member (id, name, email) VALUES (1, '보내는사람', 'sender@test.com');
INSERT INTO member (id, name, email) VALUES (2, '받는사람', 'receiver@test.com');
INSERT INTO option (id, name, quantity, product_id) VALUES (1, '기본', 10, 1);
INSERT INTO option (id, name, quantity, product_id) VALUES (2, '소량', 2, 1);
