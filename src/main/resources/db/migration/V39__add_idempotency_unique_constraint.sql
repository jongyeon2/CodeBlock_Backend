-- V40: 멱등키 유니크 제약 추가
-- (user_id, idempotency_key) 조합에 유니크 인덱스 추가로 중복 요청 방지

ALTER TABLE idempotency_keys
    ADD UNIQUE KEY uq_idem_user_key (user_id, idempotency_key);
