package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.response.PaymentResponse;
import com.hust.orderservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminTransactionController {

    private final PaymentService paymentService;

    /**
     * Lấy danh sách giao dịch thanh toán phân trang dành cho Admin.
     * Phục vụ tra cứu/đối soát đơn hàng & thanh toán ngân hàng.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<PaymentResponse>>> getAllTransactions(
            @PageableDefault(size = 15) Pageable pageable
    ) {
        log.info("Admin query: Fetching paginated list of all bank transactions: {}", pageable);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<PaymentResponse>>builder()
                        .success(true)
                        .payload(paymentService.getAllPaymentsForAdmin(pageable))
                        .build()
        );
    }

    /**
     * Xem chi tiết siêu dữ liệu của một Giao dịch qua Gateway ngân hàng cụ thể.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getTransactionById(@PathVariable String id) {
        log.info("Admin query: Fetching specific transaction technical audit metadata for ID: {}", id);
        return ResponseEntity.ok(
                ApiResponse.<PaymentResponse>builder()
                        .success(true)
                        .payload(paymentService.getPaymentById(id))
                        .build()
        );
    }
}
