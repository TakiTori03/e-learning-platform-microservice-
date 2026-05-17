package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.QuizRequest;
import com.hust.assessmentservice.dto.response.QuizResponse;
import com.hust.assessmentservice.entity.Quiz;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfiguration.class, uses = {QuestionMapper.class})
public interface QuizMapper extends BaseMapper<Quiz, QuizRequest, QuizResponse> {

    @AfterMapping
    default void establishBidirectionalRelationships(@MappingTarget Quiz quiz) {
        if (quiz.getQuestions() != null) {
            quiz.getQuestions().forEach(question -> question.setQuiz(quiz));
        }
    }
}
