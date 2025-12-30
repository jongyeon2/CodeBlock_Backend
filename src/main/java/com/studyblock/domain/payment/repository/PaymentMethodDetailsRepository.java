package com.studyblock.domain.payment.repository;

import com.studyblock.domain.payment.entity.PaymentMethodDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodDetailsRepository extends JpaRepository<PaymentMethodDetails, Long> {
    List<PaymentMethodDetails> findByPayment_Id(Long paymentId);
    Optional<PaymentMethodDetails> findByPayment_IdAndMethodType(Long paymentId, PaymentMethodDetails.MethodType methodType);
}

