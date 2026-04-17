package com.hust.orderservice.service;

import java.util.Map;

public interface VNPAYService {
    String createPaymentUrl(String orderId, long amount, String orderInfo, String ipAddress);
    boolean verifyCallback(Map<String, String> queryParams);
}
