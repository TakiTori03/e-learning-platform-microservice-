package com.hust.interactionservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;
import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {
    FeedbackResponse createFeedback(FeedbackRequest request);
    ListResponse<FeedbackResponse> getMyFeedbacks(Pageable pageable);
    FeedbackResponse getFeedbackDetail(String id);
    
    // Admin functions
    ListResponse<FeedbackResponse> getAllFeedbacks(String keyword, FeedbackStatus status, Pageable pageable);
    FeedbackResponse updateFeedbackStatus(String id, FeedbackStatus status);
    FeedbackReplyResponse replyFeedback(String feedbackId, FeedbackReplyRequest request);
    void deleteFeedback(String id);
}
