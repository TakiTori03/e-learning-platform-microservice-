package com.hust.orderservice.service;



import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.request.OrderRequest;
import com.hust.orderservice.dto.response.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    OrderResponse getOrderById(String id);
   ListResponse<OrderResponse> getOrdersByUserId(String userId, Pageable pageable);
    java.util.Map<String, Long> getEnrollmentCountsBulk(List<String> courseIds);
    boolean checkIfBought(String userId, String courseId);
    java.util.Map<String, Boolean> checkIfBoughtBulk(String userId, List<String> courseIds);
    ListResponse<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse updateOrderStatus(String orderId, String status);
}
