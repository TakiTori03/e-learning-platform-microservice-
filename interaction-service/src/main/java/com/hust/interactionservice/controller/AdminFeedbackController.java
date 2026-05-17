package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ListResponse<FeedbackResponse>>> getAllFeedbacks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.hust.interactionservice.constant.FeedbackStatus status,
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<FeedbackResponse>>builder()
                        .success(true)
                        .payload(feedbackService.getAllFeedbacks(q, status, pageable))
                        .build()
        );
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackReplyResponse>> replyFeedback(
            @PathVariable String id,
            @RequestBody @Valid FeedbackReplyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackReplyResponse>builder()
                        .success(true)
                        .payload(feedbackService.replyFeedback(id, request))
                        .build()
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedbackStatus(
            @PathVariable String id,
            @RequestParam com.hust.interactionservice.constant.FeedbackStatus status) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.updateFeedbackStatus(id, status))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(@PathVariable String id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Feedback deleted successfully")
                        .build()
        );
    }
}
