package com.hust.assessmentservice.service;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.entity.enums.TargetType;

import java.util.List;

public interface QuizService {
    QuizResponse createQuiz(QuizRequest request);
    QuizResponse updateQuiz(String id, QuizRequest request);
    QuizResponse getQuizById(String id);
    List<QuizResponse> getQuizzesByTarget(String targetId, TargetType targetType);
    void deleteQuiz(String id);
}

