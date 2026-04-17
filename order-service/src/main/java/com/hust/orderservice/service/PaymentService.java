package com.hust.orderservice.service;

import java.util.Map;

public interface PaymentService {
    Map<String, String> processVNPAYIPN(Map<String, String> params);
    String processVNPAYReturn(Map<String, String> params);
}
