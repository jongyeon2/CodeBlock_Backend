-- V7: 성능 최적화를 위한 추가 인덱스

-- 복합 인덱스 추가
CREATE INDEX idx_user_status_creator ON user (status, is_creator);
CREATE INDEX idx_lecture_course_status ON lecture (course_id, status, sequence);
CREATE INDEX idx_order_user_status ON orders (user_id, status, created_at);
CREATE INDEX idx_payment_order_status ON payments (orders_id, status, created_at);
CREATE INDEX idx_ledger_wallet_direction ON wallet_ledger (wallet_id, direction, created_at);
CREATE INDEX idx_ownership_user_status ON lecture_ownership (user_id, status);
CREATE INDEX idx_cookie_batch_user_type ON cookie_batch (user_id, cookie_type, is_active);
CREATE INDEX idx_post_board_status ON post (board_id, status, created_at);
CREATE INDEX idx_comment_post_status ON comment (post_id, status, created_at);

-- 풀텍스트 인덱스 (검색 성능 향상)
ALTER TABLE course ADD FULLTEXT INDEX ft_title (title);
ALTER TABLE lecture ADD FULLTEXT INDEX ft_title_description (title, description);
ALTER TABLE post ADD FULLTEXT INDEX ft_title_content (title, original_content);
