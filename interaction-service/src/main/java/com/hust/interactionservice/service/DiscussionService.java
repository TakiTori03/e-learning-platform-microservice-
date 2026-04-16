package com.hust.interactionservice.service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.interactionservice.dto.request.DiscussionRequest;
import com.hust.interactionservice.dto.response.DiscussionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DiscussionService {
    DiscussionResponse createDiscussion(DiscussionRequest request);
    DiscussionResponse getDiscussionById(String id);
    ListResponse<DiscussionResponse> getDiscussionTreeByLesson(String lessonId, Pageable pageable);
    ListResponse<DiscussionResponse> getDiscussionTreeBySection(String sectionId, Pageable pageable);
    DiscussionResponse updateDiscussion(String id, String content);
    void deleteDiscussion(String discussionId);
}
