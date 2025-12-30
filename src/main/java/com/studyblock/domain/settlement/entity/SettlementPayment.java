package com.studyblock.domain.settlement.entity;

import com.studyblock.domain.settlement.enums.PaymentMethod;
import com.studyblock.domain.settlement.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_ledger_id", nullable = false)
    private SettlementLedger settlementLedger;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "bank_account_info", length = 500)
    private String bankAccountInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public SettlementPayment(SettlementLedger settlementLedger,
                            LocalDateTime paymentDate,
                            PaymentMethod paymentMethod,
                            String bankAccountInfo,
                            PaymentStatus status,
                            String confirmationNumber,
                            String notes) {
        this.settlementLedger = settlementLedger;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.bankAccountInfo = bankAccountInfo;
        if (status != null) this.status = status;
        this.confirmationNumber = confirmationNumber;
        this.notes = notes;
    }

    // 비즈니스 메서드

    /**
     * 지급 완료 처리
     * @param confirmationNumber 확인 번호 (선택사항, null이면 자동 생성)
     */
    public void complete(String confirmationNumber) {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("이미 지급 완료된 항목입니다");
        }
        this.status = PaymentStatus.COMPLETED;
        // 확인 번호가 없으면 자동 생성 (지급일시 기반)
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            this.confirmationNumber = generateConfirmationNumber();
        } else {
            this.confirmationNumber = confirmationNumber;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 확인 번호 자동 생성 (지급일시 기반)
     * 형식: PAY-YYYYMMDD-HHMMSS-{id 또는 timestamp}
     */
    private String generateConfirmationNumber() {
        String timestamp = LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        // ID가 있으면 ID 사용, 없으면 timestamp 사용 (거의 발생하지 않지만 안전장치)
        String identifier = this.id != null ? String.valueOf(this.id) : String.valueOf(System.currentTimeMillis());
        return String.format("PAY-%s-%s", timestamp, identifier);
    }

    /**
     * 지급 실패 처리
     * @param reason 실패 사유
     */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "실패: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 지급 취소 처리
     * @param reason 취소 사유
     */
    public void cancel(String reason) {
        if (this.status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("지급 완료된 항목은 취소할 수 없습니다");
        }
        this.status = PaymentStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "취소: " + reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 지급 완료 여부
     */
    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    /**
     * 은행 계좌 정보 업데이트
     * @param bankAccountInfo 은행 계좌 정보
     */
    public void updateBankAccountInfo(String bankAccountInfo) {
        this.bankAccountInfo = bankAccountInfo;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 메모 추가
     * @param note 추가할 메모
     */
    public void addNote(String note) {
        this.notes = (this.notes != null ? this.notes + "\n" : "") + note;
        this.updatedAt = LocalDateTime.now();
    }
}
