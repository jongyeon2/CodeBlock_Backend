package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.PaymentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMetadataRepository extends JpaRepository<PaymentMetadata, Long> {
    Optional<PaymentMetadata> findByPayment_Id(Long paymentId);
}

