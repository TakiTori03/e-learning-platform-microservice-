package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.request.SubmissionRequest;
import com.hust.assessmentservice.dto.response.SubmissionResponse;

import java.util.List;

public interface SubmissionService {
    SubmissionResponse submitQuiz(String userId, SubmissionRequest request);
    List<SubmissionResponse> getSubmissionsByUser(String userId);
}
