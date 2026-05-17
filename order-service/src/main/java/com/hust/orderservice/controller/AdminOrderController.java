package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<OrderResponse>>> getAllOrders(
            @PageableDefault Pageable pageable) {
        log.info("Admin fetching all orders with pagination: {}", pageable);
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<OrderResponse>>builder()
                        .success(true)
                        .payload(orderService.getAllOrders(pageable))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String id) {
        log.info("Admin fetching details for Order ID: {}", id);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .payload(orderService.getOrderById(id))
                        .build()
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status) {
        log.info("Admin updating status of Order {} to {}", id, status);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .payload(orderService.updateOrderStatus(id, status))
                        .build()
        );
    }
}
