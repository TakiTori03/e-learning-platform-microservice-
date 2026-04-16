package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.DiscussionRequest;
import com.hust.interactionservice.dto.response.DiscussionResponse;
import com.hust.interactionservice.entity.Discussion;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface DiscussionMapper extends BaseMapper<Discussion, DiscussionRequest, DiscussionResponse> {
}
