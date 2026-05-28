package com.hust.assessmentservice.service.impl;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.entity.enums.TargetType;
import com.hust.assessmentservice.mapper.QuizMapper;
import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.assessmentservice.service.QuizService;
import com.hust.commonlibrary.annotation.CheckCourseOwner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;

    @Override
    @Transactional
    @CheckCourseOwner(courseId = "#request.courseId") // 🪄 AOP: Check trực tiếp khi tạo đề mới
    public QuizResponse createQuiz(QuizRequest request) {
        log.info("Creating new Quiz via MapStruct for Target ID: {}, Type: {}", request.getTargetId(), request.getTargetType());
        
        // Map DTO hierarchy to Entity graph (AfterMapping auto-wires bidirectional backlinks!)
        Quiz quiz = quizMapper.requestToEntity(request);
        
        // Persist cascading tree
        Quiz savedQuiz = quizRepository.save(quiz);
        
        // Map to clean Response DTO (auto-hides isCorrect!)
        return quizMapper.entityToResponse(savedQuiz);
    }

    @Override
    @Transactional
    @CheckCourseOwner(domainId = "#id", resolver = "quizResolver") // 🪄 AOP: Tự động lấy DB check trước khi sửa đề!
    public QuizResponse updateQuiz(String id, QuizRequest request) {
        log.info("Updating Quiz {} via MapStruct partialUpdate", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));

        // Use MapStruct partialUpdate to elegantly apply new values! (This automatically calls @AfterMapping)
        quiz = quizMapper.partialUpdate(quiz, request);

        return quizMapper.entityToResponse(quizRepository.save(quiz));
    }

    @Override
    @Transactional(readOnly = true)
    public QuizResponse getQuizById(String id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));
        return quizMapper.entityToResponse(quiz);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizResponse> getQuizzesByTarget(String targetId, TargetType targetType) {
        return quizRepository.findByTargetIdAndTargetType(targetId, targetType)
                .map(quizMapper::entityToResponse)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    @Transactional
    @CheckCourseOwner(domainId = "#id", resolver = "quizResolver")
    public void deleteQuiz(String id) {
        log.info("Deleting Quiz {}", id);
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + id));
        quizRepository.delete(quiz);
    }
}

