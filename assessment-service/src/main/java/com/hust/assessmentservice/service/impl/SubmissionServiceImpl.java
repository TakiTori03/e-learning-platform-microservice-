package com.hust.assessmentservice.service.impl;

import com.hust.assessmentservice.dto.request.AnswerRequest;
import com.hust.assessmentservice.dto.request.SubmissionRequest;
import com.hust.assessmentservice.dto.response.SubmissionResponse;
import com.hust.assessmentservice.entity.Option;
import com.hust.assessmentservice.entity.Question;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.assessmentservice.entity.StudentAnswer;
import com.hust.assessmentservice.entity.Submission;
import com.hust.assessmentservice.entity.enums.SubmissionStatus;
import com.hust.assessmentservice.mapper.SubmissionMapper;
import com.hust.assessmentservice.repository.QuizRepository;
import com.hust.assessmentservice.repository.SubmissionRepository;
import com.hust.assessmentservice.service.SubmissionService;
import com.hust.commonlibrary.event.AssessmentSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final SubmissionMapper submissionMapper;
    private final ApplicationEventPublisher eventPublisher; 

    @Override
    @Transactional
    public SubmissionResponse submitQuiz(String userId, SubmissionRequest request) {
        log.info("Processing exam submission for User: {}, Quiz: {}", userId, request.getQuizId());

        // 2. Fetch the target master quiz
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found with id: " + request.getQuizId()));

        // 🛑 CHẶN KHÔNG CHO NỘP LẠI QUIZ (CHỈ LÀM 1 LẦN)
        boolean alreadySubmitted = submissionRepository.existsByUserIdAndQuizId(userId, quiz.getId());
        if (alreadySubmitted) {
            throw new RuntimeException("You have already submitted this Quiz!");
        }

        // Compile question HashMap for O(1) lookup
        Map<String, Question> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        double finalTotalEarnedPoints = 0.0;
        double maxPossiblePoints = quiz.getQuestions().stream()
                .mapToDouble(q -> q.getPoint() != null ? q.getPoint() : 0.0)
                .sum();

        // 3. Init submission record
        Submission submission = Submission.builder()
                .quizId(quiz.getId())
                .userId(userId)
                .submittedAt(Instant.now())
                .build();

        // 4. Run robust grading logic with cheat prevention and grouped scoring
        if (request.getAnswers() != null) {
            // Construct persistence entities for database tracking
            List<StudentAnswer> studentAnswers = request.getAnswers().stream().map(answerReq -> {
                String qId = answerReq.getQuestionId();
                String optId = answerReq.getOptionId();

                Question question = questionMap.get(qId);
                if (question == null) {
                    throw new IllegalArgumentException("Security violation: Question " + qId + " does not belong to this Quiz!");
                }

                question.getOptions().stream()
                        .filter(o -> o.getId().equals(optId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Security violation: Option " + optId + " is invalid for Question " + qId));

                return StudentAnswer.builder()
                        .submission(submission)
                        .questionId(qId)
                        .optionId(optId)
                        .build();
            }).toList();

            submission.setAnswers(studentAnswers);

            // Run advanced grading grouped by question to support partial correct & full penalty logic
            Map<String, List<String>> chosenOptionIdsByQuestion = request.getAnswers().stream()
                    .collect(Collectors.groupingBy(
                            AnswerRequest::getQuestionId,
                            Collectors.mapping(AnswerRequest::getOptionId, Collectors.toList())
                    ));

            for (Map.Entry<String, List<String>> entry : chosenOptionIdsByQuestion.entrySet()) {
                String questionId = entry.getKey();
                List<String> selectedOptionIds = entry.getValue();

                Question question = questionMap.get(questionId);
                if (question == null) continue; 

                List<Option> allOptions = question.getOptions();
                long totalCorrectInDb = allOptions.stream().filter(Option::isCorrect).count();

                long countCorrectChosen = 0;
                long countIncorrectChosen = 0;

                for (String selOptId : selectedOptionIds) {
                    Option opt = allOptions.stream().filter(o -> o.getId().equals(selOptId)).findFirst().orElse(null);
                    if (opt != null) {
                        if (opt.isCorrect()) {
                            countCorrectChosen++;
                        } else {
                            countIncorrectChosen++;
                        }
                    }
                }

                // 📐 Smart Custom Rule: 
                // 1. If EVEN ONE incorrect answer is chosen -> INSTANT ZERO points for this question.
                // 2. Otherwise: Each correct yields (Question Point / Total Correct Answers Defined in DB)
                if (countIncorrectChosen == 0 && totalCorrectInDb > 0) {
                    double qPoint = question.getPoint() != null ? question.getPoint() : 0.0;
                    double earned = countCorrectChosen * (qPoint / totalCorrectInDb);
                    finalTotalEarnedPoints += earned;
                }
            }
        }

        // 5. Establish SUBMITTED status (Completion-based tracking)
        submission.setScore(finalTotalEarnedPoints);
        submission.setStatus(SubmissionStatus.SUBMITTED);

        // 6. Commit results securely to DB
        Submission savedSubmission = submissionRepository.save(submission);
        log.info("Submission successfully committed to Database. ID: {}, Score: {}, Outcome: {}", 
                savedSubmission.getId(), finalTotalEarnedPoints, SubmissionStatus.SUBMITTED);

        // 7. Fire Local Spring Application Event (Decoupled, Safe, Fast!)
        // This triggers AssessmentSubmittedEventListener asynchronously ONLY IF the transaction above commits.
        AssessmentSubmittedEvent localEvent = AssessmentSubmittedEvent.builder()
                .submissionId(savedSubmission.getId())
                .assessmentId(quiz.getId())
                .assessmentType("QUIZ")
                .userId(userId)
                .targetId(quiz.getTargetId())
                .targetType(quiz.getTargetType() != null ? quiz.getTargetType().name() : null)
                .submittedAt(savedSubmission.getSubmittedAt())
                .build();

        log.info("Shouting local AssessmentSubmittedEvent into Memory Bus for decoupling...");
        eventPublisher.publishEvent(localEvent);

        // 8. Assemble secure response
        SubmissionResponse response = submissionMapper.entityToResponse(savedSubmission);
        response.setTotalMaxScore(maxPossiblePoints);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByUser(String userId) {
        List<Submission> submissions = submissionRepository.findByUserId(userId);
        return submissions.stream()
                .map(submissionMapper::entityToResponse)
                .toList();
    }
}
