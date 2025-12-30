-- V19 : 
-- 1. category 테이블 생성
-- 2. course와 category 의 연결다리 course_category 생성
-- 3. user와 category의 연결다리 user_category 생성
-- 4. course에 category컬럼 삭제

-- 1. CATEGORY(부모 카테고리가 삭제되면 자식의 parent_id를 NULL로 바꿔서 “고아 방지”)
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    parent_id BIGINT NULL,
    order_no BIGINT NOT NULL DEFAULT 0,
    depth TINYINT NOT NULL COMMENT '카테고리 깊이 (0:대분류, 1:소분류)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id)
        REFERENCES category(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. COURSE_CATEGORY
CREATE TABLE course_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '코스-카테고리 매핑 PK',
    course_id BIGINT NOT NULL COMMENT '강의 FK',
    category_id BIGINT NOT NULL COMMENT '카테고리 FK',
    CONSTRAINT fk_coursecategory_course 
        FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_coursecategory_category 
        FOREIGN KEY (category_id) REFERENCES category(id)
        ON DELETE CASCADE,
    UNIQUE (course_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_coursecategory_course_id ON course_category(course_id);
CREATE INDEX idx_coursecategory_category_id ON course_category(category_id);


-- 3. USER_CATEGORY
CREATE TABLE user_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '유저-카테고리 매핑 PK',
    user_id BIGINT NOT NULL COMMENT '유저 FK',
    category_id BIGINT NOT NULL COMMENT '카테고리 FK',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    CONSTRAINT fk_usercategory_user 
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_usercategory_category 
        FOREIGN KEY (category_id) REFERENCES category(id)
        ON DELETE CASCADE,
    UNIQUE (user_id, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_usercategory_user_id ON user_category(user_id);
CREATE INDEX idx_usercategory_category_id ON user_category(category_id);


-- 4. COURSE 테이블 category 컬럼 삭제
ALTER TABLE course
DROP COLUMN category;
