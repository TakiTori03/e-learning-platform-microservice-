package com.hust.orderservice.controller.internal;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/enrollment-counts")
    public ApiResponse<Map<String, Long>> getEnrollmentCountsBulk(@RequestParam List<String> courseIds) {
        return ApiResponse.<Map<String, Long>>builder()
                .success(true)
                .payload(orderService.getEnrollmentCountsBulk(courseIds))
                .build();
    }

    @GetMapping("/check-bought")
    public ApiResponse<Boolean> checkIfBought(@RequestParam String userId, @RequestParam String courseId) {
        return ApiResponse.<Boolean>builder()
                .success(true)
                .payload(orderService.checkIfBought(userId, courseId))
                .build();
    }

    @GetMapping("/check-bought-bulk")
    public ApiResponse<Map<String, Boolean>> checkIfBoughtBulk(
            @RequestParam String userId, 
            @RequestParam List<String> courseIds) {
        return ApiResponse.<Map<String, Boolean>>builder()
                .success(true)
                .payload(orderService.checkIfBoughtBulk(userId, courseIds))
                .build();
    }
}
