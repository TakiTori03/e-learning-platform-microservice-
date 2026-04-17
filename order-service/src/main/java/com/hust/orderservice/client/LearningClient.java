package com.hust.orderservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "learning-service")
public interface LearningClient {

    @PostMapping("/internal/learning/enroll-bulk")
    ApiResponse<Void> enrollStudentBulk(@RequestBody EnrollmentBulkRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class EnrollmentBulkRequest {
        private String userId;
        private List<String> courseIds;
    }
}
