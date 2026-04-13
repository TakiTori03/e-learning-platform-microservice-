package com.hust.courseservice.controller;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @PostMapping("/create")
    public ApiResponse<SectionResponse> create(@RequestBody SectionRequest request) {
        return ApiResponse.<SectionResponse>builder()
                .success(true)
                .payload(sectionService.create(request))
                .build();
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<SectionResponse>> getByCourseId(@PathVariable String courseId) {
        return ApiResponse.<List<SectionResponse>>builder()
                .success(true)
                .payload(sectionService.getByCourseId(courseId))
                .build();
    }
}
