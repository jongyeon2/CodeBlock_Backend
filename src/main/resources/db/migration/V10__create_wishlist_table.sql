-- 찜 강의 테이블 생성
CREATE TABLE wishlist (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '찜 강의 PK',
                        user_id BIGINT NOT NULL COMMENT '유저 FK',
                        lecture_id BIGINT NOT NULL COMMENT '강의 FK',

                        CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id)
                            REFERENCES user(id)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE,

                        CONSTRAINT fk_wishlist_lecture FOREIGN KEY (lecture_id)
                            REFERENCES lecture(id)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE,

                        CONSTRAINT uc_user_lecture UNIQUE (user_id, lecture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
