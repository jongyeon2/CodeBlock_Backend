package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.CashReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashReceiptRepository extends JpaRepository<CashReceipt, Long> {
    Optional<CashReceipt> findByPayment_Id(Long paymentId);
}


