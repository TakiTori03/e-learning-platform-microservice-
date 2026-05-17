package com.hust.interactionservice.service.impl;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.interactionservice.constant.FeedbackStatus;
import com.hust.interactionservice.dto.request.FeedbackReplyRequest;

import com.hust.interactionservice.dto.request.FeedbackRequest;
import com.hust.interactionservice.dto.response.FeedbackReplyResponse;
import com.hust.interactionservice.dto.response.FeedbackResponse;
import com.hust.interactionservice.entity.Feedback;
import com.hust.interactionservice.entity.FeedbackReply;
import com.hust.interactionservice.mapper.FeedbackMapper;
import com.hust.interactionservice.mapper.FeedbackReplyMapper;
import com.hust.interactionservice.repository.FeedbackReplyRepository;
import com.hust.interactionservice.repository.FeedbackRepository;
import com.hust.interactionservice.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackReplyRepository feedbackReplyRepository;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackReplyMapper feedbackReplyMapper;

    @Override
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Feedback feedback = feedbackMapper.requestToEntity(request);
        feedback.setUserId(userId);
        feedback.setStatus(FeedbackStatus.PENDING);
        
        Feedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.entityToResponse(saved);
    }

    @Override
    public ListResponse<FeedbackResponse> getMyFeedbacks(Pageable pageable) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<Feedback> page = feedbackRepository.findByUserId(userId, pageable);
        
        List<FeedbackResponse> content = page.getContent().stream()
                .map(this::populateReplies)
                .collect(Collectors.toList());
                
        return ListResponse.of(content, page);
    }

    @Override
    public FeedbackResponse getFeedbackDetail(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        return populateReplies(feedback);
    }

    @Override
    public ListResponse<FeedbackResponse> getAllFeedbacks(String keyword, FeedbackStatus status, Pageable pageable) {
        Page<Feedback> page;
        if (keyword != null && !keyword.isEmpty() && status != null) {
            page = feedbackRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, status, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            page = feedbackRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        } else if (status != null) {
            page = feedbackRepository.findByStatus(status, pageable);
        } else {
            page = feedbackRepository.findAll(pageable);
        }

        List<FeedbackResponse> content = page.getContent().stream()
                .map(this::populateReplies)
                .collect(Collectors.toList());

        return ListResponse.of(content, page);
    }

    @Override
    public FeedbackResponse updateFeedbackStatus(String id, FeedbackStatus status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedback.setStatus(status);
        feedbackRepository.save(feedback);
        return populateReplies(feedback);
    }

    @Override
    public FeedbackReplyResponse replyFeedback(String feedbackId, FeedbackReplyRequest request) {
        String adminId = SecurityUtils.getCurrentUserIdOrThrow();
        
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
                
        FeedbackReply reply = feedbackReplyMapper.requestToEntity(request);
        reply.setFeedbackId(feedbackId);
        reply.setUserId(adminId);
        
        FeedbackReply saved = feedbackReplyRepository.save(reply);
        
        if (FeedbackStatus.PENDING.equals(feedback.getStatus())) {
            feedback.setStatus(FeedbackStatus.RESPONDED);
            feedbackRepository.save(feedback);
        }
        
        return feedbackReplyMapper.entityToResponse(saved);
    }

    @Override
    public void deleteFeedback(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedbackRepository.delete(feedback);
        
        List<FeedbackReply> replies = feedbackReplyRepository.findByFeedbackId(id);
        feedbackReplyRepository.deleteAll(replies);
    }
    
    private FeedbackResponse populateReplies(Feedback feedback) {
        FeedbackResponse response = feedbackMapper.entityToResponse(feedback);
        List<FeedbackReplyResponse> replies = feedbackReplyRepository.findByFeedbackId(feedback.getId())
                .stream()
                .map(feedbackReplyMapper::entityToResponse)
                .collect(Collectors.toList());
        response.setReplies(replies);
        return response;
    }
}
