package com.hust.courseservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/internal/orders/enrollment-counts")
    ApiResponse<Map<String, Long>> getEnrollmentCountsBulk(@RequestParam("courseIds") List<String> courseIds);

    @GetMapping("/internal/orders/check-bought")
    ApiResponse<Boolean> checkIfBought(@RequestParam("userId") String userId, @RequestParam("courseId") String courseId);
}
