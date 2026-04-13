package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping("/create")
    public ApiResponse<LessonResponse> create(@RequestBody LessonRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .success(true)
                .payload(lessonService.create(request))
                .build();
    }

    @GetMapping("/section/{sectionId}")
    public ApiResponse<List<LessonResponse>> getBySectionId(@PathVariable String sectionId) {
        return ApiResponse.<List<LessonResponse>>builder()
                .success(true)
                .payload(lessonService.getBySectionId(sectionId))
                .build();
    }
}
