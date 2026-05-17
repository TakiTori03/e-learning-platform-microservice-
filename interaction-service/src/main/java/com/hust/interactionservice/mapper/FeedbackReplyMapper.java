package com.hust.interactionservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.entity.FeedbackReply;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfiguration.class)
public interface FeedbackReplyMapper extends BaseMapper<FeedbackReply, FeedbackReplyRequest, FeedbackReplyResponse> {
}

