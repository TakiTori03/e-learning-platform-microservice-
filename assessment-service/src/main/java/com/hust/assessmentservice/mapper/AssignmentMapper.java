package com.hust.assessmentservice.mapper;

import com.hust.assessmentservice.dto.request.AssignmentRequest;
import com.hust.assessmentservice.dto.response.AssignmentResponse;
import com.hust.assessmentservice.entity.Assignment;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.commonlibrary.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface AssignmentMapper extends BaseMapper<Assignment, AssignmentRequest, AssignmentResponse> {
}
