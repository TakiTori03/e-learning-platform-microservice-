package com.hust.orderservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.orderservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hust.orderservice.dto.response.CourseSalesReportResponse;
import com.hust.orderservice.dto.response.OrderResponse;
import com.hust.orderservice.dto.response.RevenueReportResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    /**
     * Tương đương API: /admin/report/revenues
     */
    @GetMapping("/revenues")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenues(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "month") String groupBy // day, month, year
    ) {
        return ResponseEntity.ok(
                ApiResponse.<RevenueReportResponse>builder()
                        .success(true)
                        .payload(reportService.getRevenueReport(startDate, endDate, groupBy))
                        .build()
        );
    }

    /**
     * Tương đương API: /admin/report/course-sales
     */
    @GetMapping("/course-sales")
    public ResponseEntity<ApiResponse<List<CourseSalesReportResponse>>> getCourseSales() {
        return ResponseEntity.ok(
                ApiResponse.<List<CourseSalesReportResponse>>builder()
                        .success(true)
                        .payload(reportService.getCourseSalesReport())
                        .build()
        );
    }

    /**
     * Tương đương API: /admin/report/get-top-orders
     */
    @GetMapping("/top-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getTopOrders(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponse>>builder()
                        .success(true)
                        .payload(reportService.getTopValueOrders(limit))
                        .build()
        );
    }
}
