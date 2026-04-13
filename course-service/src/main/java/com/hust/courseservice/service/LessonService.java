package com.hust.courseservice.service;

import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;

import java.util.List;

public interface LessonService {
    LessonResponse create(LessonRequest request);
    List<LessonResponse> getBySectionId(String sectionId);
}
