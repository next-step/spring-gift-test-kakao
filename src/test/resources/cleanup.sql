-- 테스트 격리를 위한 데이터 정리
-- Foreign Key 순서 고려: option -> product -> category -> member
DELETE FROM option;
DELETE FROM product;
DELETE FROM category;
DELETE FROM member;
