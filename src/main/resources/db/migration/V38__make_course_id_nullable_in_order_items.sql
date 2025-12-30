-- V38: order_items 테이블의 course_id를 nullable로 변경
-- 작성일: 2025-10-29
-- 목적: 쿠키 충전 시 OrderItem을 생성할 수 있도록 course_id를 nullable로 변경

-- course_id 컬럼을 nullable로 변경
ALTER TABLE `order_items` MODIFY COLUMN `course_id` BIGINT NULL COMMENT '강의 ID (쿠키 충전 시 NULL)';


ALTER TABLE idempotency_keys ADD UNIQUE KEY ux_user_idem (user_id, idempotency_key);

--    - 상태값 합의: `PENDING | COMPLETED | FAILED`로 통일(현재 레포 쿼리에 USED가 있어 정리 필요)
--    - `IdempotencyKey.status`에 들어갈 문자열을 위 3개로 고정
--    - 레포의 USED 관련 메서드는 미사용이거나 이름/쿼리 수정
--    - CookieChargeService 시작부에서 선점 INSERT
--    - paymentKey를 idempotency_key로 사용
--    - `request_hash`는 요청 바디를 SHA-256 등으로 계산해 저장(옵션)
--    - 이미 존재하면 status 확인 후 멱등 응답 반환(= 기존 처리 결과 재사용) 또는 “처리 중” 처리
--    - 성공/실패 시 상태 업데이트
--    - 성공: `status=COMPLETED`, `response_snapshot=성공 JSON`, `expires_at=NOW()+7일(권장)`
--    - 실패: `status=FAILED`, `response_snapshot=에러 JSON`, `expires_at=NOW()+7일`

-- CookieChargeService 적용 위치(요약)
--    - processChargeWithToss(...) 진입 직후: tryAcquire(PENDING)
--    - 승인·주문·결제·지갑 처리 완료 직후: complete(...)
--    - 예외 catch 지점: fail(...)

-- 동시성 처리 TIP
--    - tryAcquire는 UNIQUE 제약 기반으로 INSERT → 중복이면 catch 후 find로 분기
--    - 멱등 히트(COMPLETED)면 저장된 snapshot 반환해서 프론트에 그대로 응답

-- 웹훅은 선택적
--    - 필요 시 eventId로 별도 멱등 테이블(또는 동일 테이블)에 기록

-- 이 4가지만 넣으면 운영에서 중복 적립/중복 결제 방지 품질이 확 올라갑니다. 원하시면 `IdempotencyService` 스켈레톤과 `CookieChargeService` 호출부까지 바로 추가해드릴게요.
