package com.hust.assessmentservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.assessmentservice.dto.request.SubmissionRequest;
import com.hust.assessmentservice.dto.response.SubmissionResponse;
import com.hust.assessmentservice.service.SubmissionService;
import com.hust.commonlibrary.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitQuiz(
            @RequestBody SubmissionRequest request) {
        
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        SubmissionResponse response = submissionService.submitQuiz(userId, request);
        return ResponseEntity.ok(
                ApiResponse.<SubmissionResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getMySubmissions() {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<SubmissionResponse> response = submissionService.getSubmissionsByUser(userId);
        return ResponseEntity.ok(
                ApiResponse.<List<SubmissionResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }
}
