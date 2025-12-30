-- ============================================
-- OAuth2 소셜 로그인 지원을 위한 스키마 변경
-- 작성일: 2025-10-12
-- ============================================

-- ========================================
-- 1단계: 새 컬럼 추가
-- ========================================

-- 이메일 컬럼 추가 (nullable)
ALTER TABLE `user`
    ADD COLUMN `email` VARCHAR(100) NULL
COMMENT 'OAuth2 또는 로컬 이메일'
AFTER `phone`;

-- OAuth2 제공자별 고유 ID 추가 (nullable)
ALTER TABLE `user`
    ADD COLUMN `oauth_provider_id` VARCHAR(100) NULL
COMMENT 'OAuth2 제공자가 발급한 고유 사용자 ID (카카오/구글/네이버)'
AFTER `jointype`;

-- ========================================
-- 2단계: jointype 타입 변경 (VARCHAR → TINYINT)
-- ========================================

-- 2-1. 임시 TINYINT 컬럼 생성
ALTER TABLE `user`
    ADD COLUMN `jointype_new` TINYINT NOT NULL DEFAULT 0 COMMENT '0:로컬, 1:카카오, 2:구글, 3:네이버'
AFTER `jointype`;

-- 2-2. 기존 데이터 변환
-- 'LOCAL' 문자열 → 0 숫자로 변환
UPDATE `user`
SET `jointype_new` = CASE
                         WHEN UPPER(TRIM(jointype)) = 'LOCAL' THEN 0
                         WHEN UPPER(TRIM(jointype)) = 'KAKAO' THEN 1
                         WHEN UPPER(TRIM(jointype)) = 'GOOGLE' THEN 2
                         WHEN UPPER(TRIM(jointype)) = 'NAVER' THEN 3
                         WHEN jointype IS NULL OR TRIM(jointype) = '' THEN 0
                         ELSE 0 -- 예상치 못한 값도 LOCAL(0)로 처리
    END;

-- 2-3. 변환 결과 검증 (선택사항 - 주석 해제해서 수동 확인)
-- SELECT
--     jointype as old_value,
--     jointype_new as new_value,
--     COUNT(*) as count
-- FROM `user`
-- GROUP BY jointype, jointype_new;

-- 2-4. 기존 VARCHAR 컬럼 삭제
ALTER TABLE `user` DROP COLUMN `jointype`;

-- 2-5. 새 컬럼을 원래 이름으로 변경
ALTER TABLE `user`
    CHANGE COLUMN `jointype_new` `jointype` TINYINT NOT NULL DEFAULT 0
    COMMENT '0:로컬, 1:카카오, 2:구글, 3:네이버';

-- ========================================
-- 3단계: 인덱스 추가 (성능 최적화)
-- ========================================

-- OAuth2 로그인 시 사용자 검색을 위한 복합 인덱스
-- "KAKAO + 1234567890" 조합으로 빠르게 찾기
CREATE INDEX idx_oauth_lookup
    ON `user` (`jointype`, `oauth_provider_id`);

-- 이메일 검색용 인덱스
-- 이메일 중복 체크, 이메일 찾기 등에 사용
CREATE INDEX idx_email
    ON `user` (`email`);

-- ========================================
-- 4단계: 마이그레이션 완료 확인 (주석)
-- ========================================
-- 마이그레이션 후 수동으로 실행해서 결과 확인

-- jointype 분포 확인
-- SELECT
--     jointype,
--     CASE jointype
--         WHEN 0 THEN '로컬'
--         WHEN 1 THEN '카카오'
--         WHEN 2 THEN '구글'
--         WHEN 3 THEN '네이버'
--         ELSE '알 수 없음'
--     END as join_type_name,
--     COUNT(*) as user_count
-- FROM `user`
-- GROUP BY jointype
-- ORDER BY jointype;

-- 테이블 구조 확인
-- DESCRIBE `user`;

-- 인덱스 확인
-- SHOW INDEX FROM `user`;