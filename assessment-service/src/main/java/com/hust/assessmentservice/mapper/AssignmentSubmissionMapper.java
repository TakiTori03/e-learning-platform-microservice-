package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.AssignmentSubmissionRequest;
import com.hust.assessmentservice.dto.response.AssignmentSubmissionResponse;
import com.hust.assessmentservice.entity.AssignmentSubmission;
import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface AssignmentSubmissionMapper extends BaseMapper<AssignmentSubmission, AssignmentSubmissionRequest, AssignmentSubmissionResponse> {
}
