-- 선물 보내기 인수 테스트용 초기 데이터
-- FakeGiftDelivery가 발신자만 조회하므로 발신자(ID=1)만 필요
INSERT INTO member (id, name, email) VALUES (1, '발신자', 'sender@example.com');
INSERT INTO category (id, name) VALUES (1, '전자기기');
INSERT INTO product (id, name, price, image_url, category_id)
    VALUES (1, '아이폰', 1000000, 'http://image.png', 1);
INSERT INTO option (id, name, quantity, product_id)
    VALUES (1, '128GB', 1, 1);
