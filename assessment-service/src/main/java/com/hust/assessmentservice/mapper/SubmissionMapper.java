package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.SubmissionRequest;
import com.hust.assessmentservice.dto.response.SubmissionResponse;
import com.hust.assessmentservice.entity.Submission;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface SubmissionMapper extends BaseMapper<Submission, SubmissionRequest, SubmissionResponse> {
}
