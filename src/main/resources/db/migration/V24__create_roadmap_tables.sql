-- V23: 로드맵 기능을 위한 테이블 생성
-- 1. roadmap_job: 로드맵 직군 (백엔드, 프론트엔드, 풀스택, 데이터)
-- 2. roadmap_node: 로드맵 기술 노드
-- 3. roadmap_edge: 노드 간 연결
-- 4. user_roadmap_progress: 사용자 진행 상황

-- 1. ROADMAP_JOB (로드맵 직군)
CREATE TABLE roadmap_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '직군 PK',
    job_id VARCHAR(50) NOT NULL UNIQUE COMMENT '직군 식별자 (backend, frontend, fullstack, data)',
    title VARCHAR(100) NOT NULL COMMENT '직군 제목 (예: 백엔드 개발자)',
    description TEXT NULL COMMENT '직군 설명',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
    display_order INT NOT NULL DEFAULT 0 COMMENT '노출 순서',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_job_id (job_id),
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. ROADMAP_NODE (로드맵 기술 노드)
CREATE TABLE roadmap_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '노드 PK',
    node_id VARCHAR(50) NOT NULL UNIQUE COMMENT '노드 식별자 (backend-1, frontend-1)',
    roadmap_job_id BIGINT NOT NULL COMMENT '직군 FK',
    label VARCHAR(100) NOT NULL COMMENT '기술명 (예: Java 기초)',
    description TEXT NULL COMMENT '기술 설명',
    category_id BIGINT NULL COMMENT '연결된 강의 카테고리 FK',
    level TINYINT NOT NULL COMMENT '난이도 (1:기초, 2:초급, 3:중급, 4:고급)',
    estimated_hours INT NULL COMMENT '예상 학습 시간',
    position_x INT NOT NULL COMMENT 'React Flow X 좌표',
    position_y INT NOT NULL COMMENT 'React Flow Y 좌표',
    node_type VARCHAR(50) NOT NULL DEFAULT 'techNode' COMMENT '노드 타입',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmapnode_job
        FOREIGN KEY (roadmap_job_id) REFERENCES roadmap_job(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_roadmapnode_category
        FOREIGN KEY (category_id) REFERENCES category(id)
        ON DELETE SET NULL,
    INDEX idx_roadmap_job_id (roadmap_job_id),
    INDEX idx_category_id (category_id),
    INDEX idx_level (level),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. ROADMAP_EDGE (노드 간 연결)
CREATE TABLE roadmap_edge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '엣지 PK',
    edge_id VARCHAR(50) NOT NULL UNIQUE COMMENT '엣지 식별자 (e1-2)',
    roadmap_job_id BIGINT NOT NULL COMMENT '직군 FK',
    source_node_id BIGINT NOT NULL COMMENT '출발 노드 FK',
    target_node_id BIGINT NOT NULL COMMENT '도착 노드 FK',
    is_animated TINYINT(1) NOT NULL DEFAULT 1 COMMENT '애니메이션 여부',
    edge_type VARCHAR(50) NOT NULL DEFAULT 'smoothstep' COMMENT '엣지 타입',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_roadmapedge_job
        FOREIGN KEY (roadmap_job_id) REFERENCES roadmap_job(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_roadmapedge_source
        FOREIGN KEY (source_node_id) REFERENCES roadmap_node(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_roadmapedge_target
        FOREIGN KEY (target_node_id) REFERENCES roadmap_node(id)
        ON DELETE CASCADE,
    INDEX idx_roadmap_job_id (roadmap_job_id),
    INDEX idx_source_node_id (source_node_id),
    INDEX idx_target_node_id (target_node_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. USER_ROADMAP_PROGRESS (사용자 진행 상황)
CREATE TABLE user_roadmap_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '진행 PK',
    user_id BIGINT NOT NULL COMMENT '사용자 FK',
    roadmap_node_id BIGINT NOT NULL COMMENT '노드 FK',
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'NOT_STARTED' COMMENT '진행 상태',
    completed_at DATETIME NULL COMMENT '완료 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_userroadmapprogress_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_userroadmapprogress_node
        FOREIGN KEY (roadmap_node_id) REFERENCES roadmap_node(id)
        ON DELETE CASCADE,
    UNIQUE KEY uk_user_node (user_id, roadmap_node_id),
    INDEX idx_user_id (user_id),
    INDEX idx_roadmap_node_id (roadmap_node_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. 초기 데이터 삽입 (4개 직군)
INSERT INTO roadmap_job (job_id, title, description, display_order) VALUES
('backend', '백엔드 개발자', '서버, 데이터베이스, API를 다루는 개발자', 1),
('frontend', '프론트엔드 개발자', '사용자 인터페이스를 구현하는 개발자', 2),
('fullstack', '풀스택 개발자', '프론트엔드와 백엔드를 모두 다루는 개발자', 3),
('data', '데이터 전문가', '데이터 분석 및 머신러닝 전문가', 4);

-- 불필요한 키 제거
ALTER TABLE lecture_ownership 
DROP FOREIGN KEY FK_lecture_TO_lecture_ownership_1;

ALTER TABLE lecture_ownership 
DROP INDEX uk_user_lecture;

ALTER TABLE lecture_ownership 
DROP COLUMN lecture_id;

ALTER TABLE lecture_ownership
ADD CONSTRAINT uk_user_section UNIQUE (user_id, section_id);

ALTER TABLE wishlist
DROP FOREIGN KEY FK_wishlist_lecture;

ALTER TABLE wishlist 
DROP INDEX uc_user_lecture;

ALTER TABLE wishlist
DROP INDEX idx_wishlist_lecture_created;

ALTER TABLE wishlist 
DROP COLUMN lecture_id;

ALTER TABLE wishlist
ADD CONSTRAINT uc_user_section UNIQUE (user_id, section_id);
