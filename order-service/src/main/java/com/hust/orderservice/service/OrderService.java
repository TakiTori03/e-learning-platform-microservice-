package com.hust.orderservice.service;



import com.hust.orderservice.dto.OrderRequest;
import com.hust.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(String id);
    List<OrderResponse> getOrdersByUserId(String userId);
    java.util.Map<String, Long> getEnrollmentCountsBulk(List<String> courseIds);
    boolean checkIfBought(String userId, String courseId);
}
