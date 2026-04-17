package com.hust.orderservice.service.impl;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;

import com.hust.orderservice.client.CourseClient;
import com.hust.orderservice.client.dto.CourseInternalResponse;
import com.hust.orderservice.constant.OrderStatus;
import com.hust.orderservice.dto.OrderRequest;
import com.hust.orderservice.dto.OrderResponse;
import com.hust.orderservice.entity.Order;
import com.hust.orderservice.entity.OrderItem;
import com.hust.orderservice.mapper.OrderMapper;
import com.hust.orderservice.repository.OrderRepository;

import com.hust.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CourseClient courseClient;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserEmail());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // 1. Fetch course details and calculate total
        for (String courseId : request.getCourseIds()) {
            ApiResponse<CourseInternalResponse> response = courseClient.getCourseDetail(courseId);
            
            if (response == null || !response.isSuccess() || response.getPayload() == null) {
                log.error("Course not found or service error: {}", courseId);
                throw new RuntimeException();
            }

            CourseInternalResponse course = response.getPayload();
            
            OrderItem item = OrderItem.builder()
                    .courseId(course.getId())
                    .name(course.getName())
                    .finalPrice(BigDecimal.valueOf(course.getFinalPrice()))
                    .thumbnail(course.getThumbnail())
                    .build();
            
            orderItems.add(item);
            total = total.add(item.getFinalPrice());
        }

        // 2. Build and save Order
        Order order = orderMapper.requestToEntity(request);
        order.setItems(orderItems);
        order.setTotalPrice(total);
        order.setVatFee(BigDecimal.ZERO); // Standardize or add logic if needed
        
        // If price is 0, we can set status to SUCCESS immediately
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.SUCCESS);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        return orderMapper.entityToResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException());
        return orderMapper.entityToResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::entityToResponse)
                .toList();
    }

    @Override
    public java.util.Map<String, Long> getEnrollmentCountsBulk(List<String> courseIds) {
        List<Object[]> results = orderRepository.countByCourseIdsAndStatus(courseIds, OrderStatus.SUCCESS);
        java.util.Map<String, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        res -> (String) res[0],
                        res -> (Long) res[1]
                ));
        
        courseIds.forEach(id -> countMap.putIfAbsent(id, 0L));
        return countMap;
    }

    @Override
    public boolean checkIfBought(String userId, String courseId) {
        return orderRepository.existsByUserIdAndStatusAndItems_CourseId(userId, OrderStatus.SUCCESS, courseId);
    }
}
