package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.service.PaymentService;
import com.hust.orderservice.service.VNPAYService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VNPAYService vnpayService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @GetMapping("/vnpay/create/{orderId}")
    public ApiResponse<String> createVNPAYPayment(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        long amount = order.getTotalPrice().longValue();
        String orderInfo = "Payment for order " + orderId;
        
        // Handle IP behind proxy if necessary
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        String paymentUrl = vnpayService.createPaymentUrl(orderId, amount, orderInfo, ipAddress);
        return ApiResponse.<String>builder()
                .success(true)
                .payload(paymentUrl)
                .build();
    }

    /**
     * Handle VNPay Client Redirect (Return URL)
     */
    @GetMapping("/vnpay/vnpay_return")
    public ApiResponse<String> vnpayReturn(@RequestParam Map<String, String> params) {
        String result = paymentService.processVNPAYReturn(params);
        return ApiResponse.<String>builder()
                .success("success".equals(result))
                .payload(result)
                .build();
    }

    /**
     * Handle VNPay Server-to-Server Callback (IPN)
     * This is the official status update point.
     */
    @GetMapping("/vnpay/vnpay_ipn")
    public Map<String, String> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN Received: {}", params);
        return paymentService.processVNPAYIPN(params);
    }
}
