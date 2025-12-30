-- V8: 시스템 필수 데이터

-- 기본 역할
INSERT INTO role (code, name) VALUES 
('ADMIN', '관리자'),
('USER', '일반 사용자'),
('INSTRUCTOR', '강사');

-- 기본 게시판
INSERT INTO board (name, type) VALUES 
('공지사항', 1),
('자유게시판', 0),
('FAQ', 2),
('강의 후기', 0),
('질문 게시판', 0);

-- 기본 권한 데이터
INSERT INTO permission (code, name, description) VALUES
('USER_READ', '사용자 조회', '사용자 정보를 조회할 수 있는 권한'),
('USER_WRITE', '사용자 생성/수정', '사용자 정보를 생성하거나 수정할 수 있는 권한'),
('USER_DELETE', '사용자 삭제', '사용자를 삭제할 수 있는 권한'),
('COURSE_READ', '코스 조회', '코스 정보를 조회할 수 있는 권한'),
('COURSE_WRITE', '코스 생성/수정', '코스를 생성하거나 수정할 수 있는 권한'),
('COURSE_DELETE', '코스 삭제', '코스를 삭제할 수 있는 권한'),
('LECTURE_READ', '강의 조회', '강의 정보를 조회할 수 있는 권한'),
('LECTURE_WRITE', '강의 생성/수정', '강의를 생성하거나 수정할 수 있는 권한'),
('LECTURE_DELETE', '강의 삭제', '강의를 삭제할 수 있는 권한'),
('PAYMENT_READ', '결제 조회', '결제 정보를 조회할 수 있는 권한'),
('PAYMENT_WRITE', '결제 처리', '결제를 처리할 수 있는 권한'),
('REFUND_PROCESS', '환불 처리', '환불을 처리할 수 있는 권한'),
('BOARD_MANAGE', '게시판 관리', '게시판을 관리할 수 있는 권한'),
('REPORT_MANAGE', '신고 관리', '신고를 관리할 수 있는 권한');

-- 역할별 권한 매핑
-- ADMIN: 모든 권한
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r 
CROSS JOIN permission p 
WHERE r.code = 'ADMIN';

-- USER: 기본 조회 권한만
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r 
JOIN permission p ON p.code IN ('COURSE_READ', 'LECTURE_READ')
WHERE r.code = 'USER';

-- INSTRUCTOR: 자신의 강의 관련 권한
INSERT INTO role_permission (role_id, permission_id) 
SELECT r.id, p.id 
FROM role r 
JOIN permission p ON p.code IN ('COURSE_READ', 'COURSE_WRITE', 'LECTURE_READ', 'LECTURE_WRITE', 'PAYMENT_READ')
WHERE r.code = 'INSTRUCTOR';