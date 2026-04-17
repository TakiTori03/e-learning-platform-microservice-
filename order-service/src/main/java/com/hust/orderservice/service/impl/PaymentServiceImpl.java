package com.hust.orderservice.service.impl;

import com.hust.orderservice.client.LearningClient;
import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.constant.PaymentStatus;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.entity.Payment;
import com.hust.orderservice.repository.OrderRepository;
import com.hust.orderservice.repository.PaymentRepository;
import com.hust.orderservice.service.PaymentService;
import com.hust.orderservice.service.VNPAYService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VNPAYService vnpayService;
    private final LearningClient learningClient;

    @Override
    @Transactional
    public Map<String, String> processVNPAYIPN(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        
        try {
            if (!vnpayService.verifyCallback(params)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String vnp_TxnRef = params.get("vnp_TxnRef");
            String orderId = vnp_TxnRef.split("_")[0];
            BigDecimal vnp_Amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }

            if (order.getTotalPrice().compareTo(vnp_Amount) != 0) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return response;
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            String responseCode = params.get("vnp_ResponseCode");
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
            payment.setOrder(order);
            payment.setTransactionId(params.get("vnp_TransactionNo"));
            payment.setMethod("VNPAY");
            payment.setAmount(vnp_Amount);
            payment.setBankCode(params.get("vnp_BankCode"));
            payment.setBankTranNo(params.get("vnp_BankTranNo"));
            payment.setCardType(params.get("vnp_CardType"));
            payment.setOrderInfo(params.get("vnp_OrderInfo"));
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setPayDate(Instant.now());

            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                order.setStatus(OrderStatus.SUCCESS);
                log.info("IPN Success: Order {} marked as SUCCESS", orderId);
                triggerAutoEnrollment(order);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.CANCELLED);
                log.warn("IPN Failed: Order {} marked as CANCELLED", orderId);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");

        } catch (Exception e) {
            log.error("IPN Error: ", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }

        return response;
    }

    private void triggerAutoEnrollment(Order order) {
        log.info("Triggering Bulk Auto-Enrollment for Order ID: {} and User ID: {}", order.getId(), order.getUserId());
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            java.util.List<String> courseIds = order.getItems().stream()
                    .map(com.hust.orderservice.entity.OrderItem::getCourseId)
                    .toList();

            try {
                learningClient.enrollStudentBulk(LearningClient.EnrollmentBulkRequest.builder()
                        .userId(order.getUserId())
                        .courseIds(courseIds)
                        .build());
                log.info("Successfully processed bulk enrollment for User {} with {} courses", order.getUserId(), courseIds.size());
            } catch (Exception e) {
                log.error("Failed to process bulk enrollment for User {}: {}", order.getUserId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public String processVNPAYReturn(Map<String, String> params) {
        if (!vnpayService.verifyCallback(params)) {
             return "signature_invalid";
        }
        String responseCode = params.get("vnp_ResponseCode");
        return "00".equals(responseCode) ? "success" : "failed";
    }
}
