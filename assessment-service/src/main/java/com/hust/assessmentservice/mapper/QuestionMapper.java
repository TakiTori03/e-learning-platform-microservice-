package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.QuestionRequest;
import com.hust.assessmentservice.dto.response.QuestionResponse;
import com.hust.assessmentservice.entity.Question;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfiguration.class, uses = {OptionMapper.class})
public interface QuestionMapper extends BaseMapper<Question, QuestionRequest, QuestionResponse> {

    @AfterMapping
    default void establishBidirectionalRelationships(@MappingTarget Question question) {
        if (question.getOptions() != null) {
            question.getOptions().forEach(option -> option.setQuestion(question));
        }
    }
}
