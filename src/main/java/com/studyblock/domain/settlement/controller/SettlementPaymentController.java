package com.studyblock.domain.settlement.controller;

import com.studyblock.domain.settlement.dto.ExecutePaymentRequest;
import com.studyblock.domain.settlement.dto.GenerateTaxInvoiceRequest;
import com.studyblock.domain.settlement.dto.SettlementPaymentResponse;
import com.studyblock.domain.settlement.dto.SettlementTaxInvoiceResponse;
import com.studyblock.domain.settlement.entity.SettlementPayment;
import com.studyblock.domain.settlement.entity.SettlementTaxInvoice;
import com.studyblock.domain.settlement.enums.PaymentMethod;
import com.studyblock.domain.settlement.service.SettlementPaymentService;
import com.studyblock.domain.settlement.service.SettlementTaxInvoiceService;
import com.studyblock.global.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementPaymentController {

    private final SettlementPaymentService settlementPaymentService;
    private final SettlementTaxInvoiceService settlementTaxInvoiceService;

    // ========================================
    // 정산 지급 관련 엔드포인트
    // ========================================

    //정산 지급 실행 (관리자 전용)
    @PostMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementPaymentResponse>> executePayment(
            @RequestBody ExecutePaymentRequest request) {

        try {
            SettlementPayment payment = settlementPaymentService.executePayment(
                    request.getLedgerId(),
                    PaymentMethod.BANK_TRANSFER, // 계좌이체만 사용
                    request.getBankAccountInfo(),
                    request.getNotes(),
                    request.getConfirmationNumber() // 확인 번호 (선택사항)
            );

            // 지급 실행 시 자동으로 완료 처리됨 (확인번호는 선택사항)
            return ResponseEntity.ok(CommonResponse.success(
                    "정산 지급이 생성되고 완료 처리되었습니다",
                    SettlementPaymentResponse.from(payment)
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 지급 실행 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 지급 처리 중 오류가 발생했습니다"));
        }
    }

    //정산 지급 완료 처리 (관리자 전용)
    @PutMapping("/payments/{paymentId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementPaymentResponse>> completePayment(
            @PathVariable Long paymentId,
            @RequestParam String confirmationNumber) {

        try {
            SettlementPayment payment = settlementPaymentService.completePayment(paymentId, confirmationNumber);

            return ResponseEntity.ok(CommonResponse.success(
                    "정산 지급이 완료되었습니다",
                    SettlementPaymentResponse.from(payment)
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 지급 완료 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 지급 완료 처리 중 오류가 발생했습니다"));
        }
    }

    //정산 지급 실패 처리 (관리자 전용)
    @PutMapping("/payments/{paymentId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementPaymentResponse>> failPayment(
            @PathVariable Long paymentId,
            @RequestParam String reason) {

        try {
            SettlementPayment payment = settlementPaymentService.failPayment(paymentId, reason);

            return ResponseEntity.ok(CommonResponse.success(
                    "정산 지급이 실패 처리되었습니다",
                    SettlementPaymentResponse.from(payment)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("정산 지급 실패 처리 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("정산 지급 실패 처리 중 오류가 발생했습니다"));
        }
    }

    //강사별 지급 내역 조회 (페이징 지원)
    @GetMapping("/payments/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMIN') or #instructorId == authentication.principal.id")
    public ResponseEntity<CommonResponse<Page<SettlementPaymentResponse>>> getPaymentHistory(
            @PathVariable Long instructorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        try {
            // 정렬 방향 설정
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<SettlementPayment> payments = settlementPaymentService.getPaymentHistory(instructorId, pageable);
            Page<SettlementPaymentResponse> responses = payments.map(SettlementPaymentResponse::from);

            return ResponseEntity.ok(CommonResponse.success(
                    "지급 내역을 조회했습니다",
                    responses
            ));

        } catch (Exception e) {
            log.error("지급 내역 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("지급 내역 조회 중 오류가 발생했습니다"));
        }
    }

    //지급 대기 건수 조회 (관리자 전용)
    @GetMapping("/payments/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<Long>> getPendingPaymentCount() {

        try {
            Long count = settlementPaymentService.getPendingPaymentCount();

            return ResponseEntity.ok(CommonResponse.success(
                    "지급 대기 건수를 조회했습니다",
                    count
            ));

        } catch (Exception e) {
            log.error("지급 대기 건수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("지급 대기 건수 조회 중 오류가 발생했습니다"));
        }
    }

    // ========================================
    // 세금계산서 관련 엔드포인트
    // ========================================

    //세금계산서 발행 (관리자 전용)
    @PostMapping("/tax-invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementTaxInvoiceResponse>> generateTaxInvoice(
            @RequestBody GenerateTaxInvoiceRequest request) {

        try {
            SettlementTaxInvoice taxInvoice = settlementTaxInvoiceService.generateTaxInvoice(
                    request.getPaymentId(),
                    request.getPeriodStart(),
                    request.getPeriodEnd(),
                    request.getInvoiceFileUrl()
            );

            return ResponseEntity.ok(CommonResponse.success(
                    "세금계산서가 발행되었습니다",
                    SettlementTaxInvoiceResponse.from(taxInvoice)
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("세금계산서 발행 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("세금계산서 발행 중 오류가 발생했습니다"));
        }
    }

    //세금계산서 파일 업데이트 (관리자 전용)
    @PutMapping("/tax-invoices/{taxInvoiceId}/file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementTaxInvoiceResponse>> updateTaxInvoiceFile(
            @PathVariable Long taxInvoiceId,
            @RequestParam String filePath) {

        try {
            SettlementTaxInvoice taxInvoice = settlementTaxInvoiceService.updateTaxInvoiceFile(taxInvoiceId, filePath);

            return ResponseEntity.ok(CommonResponse.success(
                    "세금계산서 파일이 업데이트되었습니다",
                    SettlementTaxInvoiceResponse.from(taxInvoice)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("세금계산서 파일 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("세금계산서 파일 업데이트 중 오류가 발생했습니다"));
        }
    }

    // /**
    //  * 세금계산서 파일 업데이트 (S3) (관리자 전용)
    //  */
    // @PutMapping("/tax-invoices/{taxInvoiceId}/file/s3")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<CommonResponse<SettlementTaxInvoiceResponse>> updateTaxInvoiceFileFromS3(
    //         @PathVariable Long taxInvoiceId,
    //         @RequestParam String s3Url) {
    //
    //     try {
    //         SettlementTaxInvoice taxInvoice = settlementService.updateTaxInvoiceFileFromS3(taxInvoiceId, s3Url);
    //
    //         return ResponseEntity.ok(CommonResponse.success(
    //                 "세금계산서 S3 파일이 업데이트되었습니다",
    //                 SettlementTaxInvoiceResponse.from(taxInvoice)
    //         ));
    //
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //                 .body(CommonResponse.error(e.getMessage()));
    //     } catch (Exception e) {
    //         log.error("세금계산서 S3 파일 업데이트 실패", e);
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //                 .body(CommonResponse.error("세금계산서 S3 파일 업데이트 중 오류가 발생했습니다"));
    //     }
    // }

    //강사별 세금계산서 조회
    @GetMapping("/tax-invoices/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMIN') or #instructorId == authentication.principal.id")
    public ResponseEntity<CommonResponse<List<SettlementTaxInvoiceResponse>>> getTaxInvoices(
            @PathVariable Long instructorId) {

        try {
            List<SettlementTaxInvoice> taxInvoices = settlementTaxInvoiceService.getTaxInvoices(instructorId);
            List<SettlementTaxInvoiceResponse> responses = taxInvoices.stream()
                    .map(SettlementTaxInvoiceResponse::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(CommonResponse.success(
                    "세금계산서 목록을 조회했습니다",
                    responses
            ));

        } catch (Exception e) {
            log.error("세금계산서 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("세금계산서 조회 중 오류가 발생했습니다"));
        }
    }

    //세금계산서 번호로 조회
    @GetMapping("/tax-invoices/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<SettlementTaxInvoiceResponse>> getTaxInvoiceByNumber(
            @PathVariable String invoiceNumber) {

        try {
            SettlementTaxInvoice taxInvoice = settlementTaxInvoiceService.getTaxInvoiceByNumber(invoiceNumber);

            return ResponseEntity.ok(CommonResponse.success(
                    "세금계산서를 조회했습니다",
                    SettlementTaxInvoiceResponse.from(taxInvoice)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("세금계산서 번호 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error("세금계산서 조회 중 오류가 발생했습니다"));
        }
    }
}
