package com.hust.assessmentservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.entity.enums.TargetType;
import com.hust.assessmentservice.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(@RequestBody QuizRequest request) {
        QuizResponse response = quizService.createQuiz(request);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable String id,
            @RequestBody QuizRequest request) {
        QuizResponse response = quizService.updateQuiz(id, request);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizById(@PathVariable String id) {
        QuizResponse response = quizService.getQuizById(id);
        return ResponseEntity.ok(
                ApiResponse.<QuizResponse>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByTarget(
            @PathVariable TargetType targetType,
            @PathVariable String targetId) {
        List<QuizResponse> response = quizService.getQuizzesByTarget(targetId, targetType);
        return ResponseEntity.ok(
                ApiResponse.<List<QuizResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByCourse(@PathVariable String courseId) {
        List<QuizResponse> response = quizService.getQuizzesByTarget(courseId, TargetType.COURSE);
        return ResponseEntity.ok(
                ApiResponse.<List<QuizResponse>>builder()
                        .success(true)
                        .payload(response)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable String id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Quiz deleted successfully")
                        .build()
        );
    }
}

