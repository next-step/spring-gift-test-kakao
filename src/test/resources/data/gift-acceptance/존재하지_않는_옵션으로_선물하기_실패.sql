-- 데이터 초기화 (자식 테이블부터 역순 삭제)
SET REFERENTIAL_INTEGRITY FALSE;
TRUNCATE TABLE wish;
TRUNCATE TABLE option;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE member;
SET REFERENTIAL_INTEGRITY TRUE;

-- 보내는 사람 (Member-Id 헤더용)
INSERT INTO member (id, name, email) VALUES (1, '보내는이', 'sender@kakao.com');

-- 옵션은 의도적으로 삽입하지 않음: 존재하지 않는 옵션 ID 조회 시 예외 발생
