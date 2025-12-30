-- V48: lecture_resource 테이블의 lecture_id를 NULL 허용으로 변경
-- 작성일: 2025-11-08
-- 목적: 섹션 자료 업로드 기능 지원 (섹션 자료는 lecture_id가 NULL)

-- ========================================
-- 배경 설명
-- ========================================
-- 기존: lecture_resource는 강의 자료만 저장 (lecture_id NOT NULL)
-- 변경: 섹션 자료도 저장 가능 (lecture_id NULL 허용)
--
-- 자료 유형:
-- 1. 강의 자료: lecture_id NOT NULL, section_id NOT NULL
--    - 특정 강의에 속하는 자료 (예: 강의 1의 코드 샘플)
--
-- 2. 섹션 자료: lecture_id NULL, section_id NOT NULL
--    - 섹션 전체에 속하는 자료 (예: 섹션 1의 실습 템플릿)

-- ========================================
-- 1단계: 사전 확인
-- ========================================

-- 현재 테이블 구조 확인 (실행 전 주석 해제)
-- DESCRIBE lecture_resource;

-- 기존 데이터 통계
-- SELECT
--     COUNT(*) AS total_count,
--     COUNT(lecture_id) AS lecture_material_count,
--     COUNT(*) - COUNT(lecture_id) AS section_material_count
-- FROM lecture_resource;

-- lecture_id가 NULL인 데이터 확인 (있으면 안 됨)
-- SELECT COUNT(*) AS unexpected_null_count
-- FROM lecture_resource
-- WHERE lecture_id IS NULL;

-- ========================================
-- 2단계: 마이그레이션 실행
-- ========================================

-- lecture_id 컬럼을 NULL 허용으로 변경
ALTER TABLE `lecture_resource`
    MODIFY COLUMN `lecture_id` BIGINT NULL COMMENT '강의 FK (섹션 자료인 경우 NULL)';

-- ========================================
-- 3단계: 변경 확인
-- ========================================

-- 테이블 구조 재확인 (실행 후 주석 해제)
-- DESCRIBE lecture_resource;

-- NULL 제약 조건 확인
-- SELECT
--     COLUMN_NAME,
--     IS_NULLABLE,
--     COLUMN_TYPE,
--     COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'lecture_resource'
--   AND COLUMN_NAME IN ('lecture_id', 'section_id');

-- ========================================
-- 4단계: 인덱스 확인 (FK 인덱스는 유지됨)
-- ========================================

-- 기존 인덱스 확인
-- SHOW INDEX FROM lecture_resource;

-- FK 제약 조건 확인
-- SELECT
--     CONSTRAINT_NAME,
--     COLUMN_NAME,
--     REFERENCED_TABLE_NAME,
--     REFERENCED_COLUMN_NAME
-- FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'lecture_resource'
--   AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ========================================
-- 5단계: 테스트 데이터 (선택사항)
-- ========================================

-- 섹션 자료 테스트 삽입 (확인 후 삭제 필요)
-- INSERT INTO lecture_resource (
--     section_id,
--     lecture_id,
--     title,
--     file_url,
--     file_type,
--     file_size,
--     description,
--     download_count,
--     sequence,
--     upload_at
-- ) VALUES (
--     1,                                  -- section_id (실제 섹션 ID 사용)
--     NULL,                               -- lecture_id (섹션 자료이므로 NULL)
--     'Test Section Material',            -- title
--     'https://study-block.s3.ap-northeast-2.amazonaws.com/materials/section-1/test.pdf',
--     'application/pdf',                  -- file_type
--     1024,                               -- file_size
--     'Test section material',            -- description
--     0,                                  -- download_count
--     1,                                  -- sequence
--     NOW()                               -- upload_at
-- );

-- 테스트 데이터 확인
-- SELECT * FROM lecture_resource WHERE lecture_id IS NULL;

-- 테스트 데이터 삭제
-- DELETE FROM lecture_resource WHERE lecture_id IS NULL;

-- ========================================
-- 롤백 스크립트 (문제 발생 시)
-- ========================================

-- ⚠️ 롤백 전 주의사항:
-- 1. 섹션 자료가 있으면 롤백 불가능 (데이터 손실 발생)
-- 2. 반드시 백업 후 롤백 수행

-- 섹션 자료 확인
-- SELECT COUNT(*) AS section_material_count
-- FROM lecture_resource
-- WHERE lecture_id IS NULL;

-- 섹션 자료 삭제 (롤백 시 필요)
-- DELETE FROM lecture_resource WHERE lecture_id IS NULL;

-- lecture_id를 NOT NULL로 복원
-- ALTER TABLE lecture_resource
-- MODIFY COLUMN lecture_id BIGINT NOT NULL COMMENT '강의 FK';

-- ========================================
-- 마이그레이션 완료
-- ========================================

-- 다음 단계:
-- 1. 애플리케이션 재시작
-- 2. Swagger에서 섹션 자료 업로드 API 테스트
--    - POST /api/materials/upload
-- 3. 에러 로그 모니터링
-- 4. 기존 강의 자료 정상 작동 확인

-- 확인 쿼리:
-- - 섹션 자료 목록: SELECT * FROM lecture_resource WHERE lecture_id IS NULL;
-- - 강의 자료 목록: SELECT * FROM lecture_resource WHERE lecture_id IS NOT NULL;
-- - 전체 자료 통계: SELECT
--     COUNT(*) AS total,
--     SUM(CASE WHEN lecture_id IS NULL THEN 1 ELSE 0 END) AS section_materials,
--     SUM(CASE WHEN lecture_id IS NOT NULL THEN 1 ELSE 0 END) AS lecture_materials
-- FROM lecture_resource;