package com.hust.orderservice.dto;


import com.hust.orderservice.constant.OrderStatus;

import com.hust.orderservice.entity.OrderItem;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String userId;
    private String userEmail;
    private String userName;
    private Double totalPrice;
    private Double vatFee;
    private String note;
    private OrderStatus status;
    private List<OrderItem> items;
    private Instant createdAt;
}
