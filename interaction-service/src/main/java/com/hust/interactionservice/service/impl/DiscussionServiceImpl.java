package com.hust.interactionservice.service.impl;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.interactionservice.dto.request.DiscussionRequest;
import com.hust.interactionservice.dto.response.DiscussionResponse;
import com.hust.interactionservice.entity.Discussion;
import com.hust.interactionservice.mapper.DiscussionMapper;
import com.hust.interactionservice.repository.DiscussionRepository;
import com.hust.interactionservice.service.DiscussionService;
import com.hust.interactionservice.utils.AppUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionRepository discussionRepository;
    private final DiscussionMapper discussionMapper;

    @Override
    public DiscussionResponse createDiscussion(DiscussionRequest request) {
        String userId = SecurityUtils.getCurrentUserIdOrThrow();
        Discussion discussion = discussionMapper.requestToEntity(request);
        discussion.setUserId(userId);
        discussion.setCode(AppUtils.generateCode("DISCUSS"));
        
        if (discussion.getParentId() != null) {
            Discussion parent = discussionRepository.findById(discussion.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
            discussion.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
            discussion.setCourseId(parent.getCourseId());
            discussion.setLessonId(parent.getLessonId());
        }

        return discussionMapper.entityToResponse(discussionRepository.save(discussion));
    }

    @Override
    public DiscussionResponse getDiscussionById(String id) {
        return discussionRepository.findById(id)
                .map(discussionMapper::entityToResponse)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));
    }

    @Override
    public ListResponse<DiscussionResponse> getDiscussionTreeByLesson(String lessonId, Pageable pageable) {
        Page<Discussion> rootPage = discussionRepository.findByLessonIdAndParentIdIsNull(lessonId, pageable);
        return buildPagedTree(rootPage);
    }

    @Override
    public ListResponse<DiscussionResponse> getDiscussionTreeBySection(String sectionId, Pageable pageable) {
        Page<Discussion> rootPage = discussionRepository.findBySectionIdAndParentIdIsNull(sectionId, pageable);
        return buildPagedTree(rootPage);
    }

    @Override
    public DiscussionResponse updateDiscussion(String id, String content) {
        Discussion discussion = discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));
        
        discussion.setContent(content);
        return discussionMapper.entityToResponse(discussionRepository.save(discussion));
    }

    private ListResponse<DiscussionResponse> buildPagedTree(Page<Discussion> rootPage) {
        List<String> rootIds = rootPage.getContent().stream()
                .map(Discussion::getId)
                .collect(Collectors.toList());

        List<Discussion> allRelated = discussionRepository.findByRootIdIn(rootIds);
        
        List<Discussion> combined = new ArrayList<>(rootPage.getContent());
        combined.addAll(allRelated);

        List<DiscussionResponse> allResponses = combined.stream()
                .map(discussionMapper::entityToResponse)
                .collect(Collectors.toList());

        Map<String, List<DiscussionResponse>> groupedByParent = allResponses.stream()
                .filter(res -> res.getParentId() != null)
                .collect(Collectors.groupingBy(DiscussionResponse::getParentId));

        List<DiscussionResponse> rootResponses = allResponses.stream()
                .filter(res -> rootIds.contains(res.getId()))
                .collect(Collectors.toList());

        allResponses.forEach(res -> res.setReplies(groupedByParent.get(res.getId())));

        return ListResponse.of(rootResponses, rootPage);
    }

    @Override
    public void deleteDiscussion(String discussionId) {
        discussionRepository.deleteById(discussionId);
    }
}
