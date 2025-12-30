package com.studyblock.domain.idempotency.entity;

import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    @Column(name = "response_snapshot", columnDefinition = "JSON")
    private String responseSnapshot;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    public IdempotencyKey(User user,
                          String idempotencyKey,
                          String requestHash,
                          String responseSnapshot,
                          String status,
                          LocalDateTime expiresAt) {
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseSnapshot = responseSnapshot;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    // 비즈니스 메서드
    //멱등성 키를 사용 완료 상태로 변경 및 응답 캐싱
    //@param responseSnapshot 응답 내용 (JSON 형태)
    public void markAsUsed(String responseSnapshot) {
        this.status = "COMPLETED";
        if (responseSnapshot != null) {
            this.responseSnapshot = responseSnapshot;
        }
        // 완료 후 7일 후 만료로 설정 (마이그레이션 주석 권장사항)
        this.expiresAt = java.time.LocalDateTime.now().plusDays(7);
    }

    //멱등성 키를 실패 상태로 변경 및 에러 캐싱
    //@param errorSnapshot 에러 내용 (JSON 형태)
    public void markAsFailed(String errorSnapshot) {
        this.status = "FAILED";
        if (errorSnapshot != null) {
            this.responseSnapshot = errorSnapshot;
        }
        // 실패 후 7일 후 만료로 설정 (마이그레이션 주석 권장사항)
        this.expiresAt = java.time.LocalDateTime.now().plusDays(7);
    }

    //멱등성 키 상태 변경
    //@param status 새 상태
    public void changeStatus(String status) {
        this.status = status;
    }

    //요청 해시 설정
    //@param requestHash 요청 내용의 해시값
    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    //응답 캐시 설정
    //@param responseSnapshot 응답 내용 (JSON 형태)
    public void setResponseSnapshot(String responseSnapshot) {
        this.responseSnapshot = responseSnapshot;
    }

    //만료 시간 설정
    //@param expiresAt 만료 시간
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    //멱등성 키가 만료되었는지 확인
    //@return 만료되었으면 true
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    //멱등성 키가 완료되었는지 확인
    //@return 완료되었으면 true
    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    //멱등성 키가 대기 중인지 확인
    //@return 대기 중이면 true
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }
}


