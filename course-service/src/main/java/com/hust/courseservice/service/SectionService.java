package com.hust.courseservice.service;

import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;

import java.util.List;

public interface SectionService {
    SectionResponse create(SectionRequest request);
    List<SectionResponse> getByCourseId(String courseId);
}
