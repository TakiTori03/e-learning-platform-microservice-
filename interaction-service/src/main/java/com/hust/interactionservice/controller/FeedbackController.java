package com.hust.interactionservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(@RequestBody @Valid FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.createFeedback(request))
                        .build()
        );
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<ListResponse<FeedbackResponse>>> getMyFeedbacks(
            @PageableDefault Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<ListResponse<FeedbackResponse>>builder()
                        .success(true)
                        .payload(feedbackService.getMyFeedbacks(pageable))
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedbackDetail(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.<FeedbackResponse>builder()
                        .success(true)
                        .payload(feedbackService.getFeedbackDetail(id))
                        .build()
        );
    }
}
