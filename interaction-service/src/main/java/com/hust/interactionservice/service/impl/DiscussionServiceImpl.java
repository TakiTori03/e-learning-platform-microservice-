package com.hust.interactionservice.service.impl;

import com.hust.commonlibrary.constant.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.service.RedisService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final RedisService redisService;

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
            discussion.setSectionId(parent.getSectionId());
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
        
        // 🔐 Chốt chặn bảo mật: Chỉ cho phép chính chủ sửa bài thảo luận của mình
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!discussion.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Access denied: You are not authorized to update this discussion!");
        }

        discussion.setContent(content);
        return discussionMapper.entityToResponse(discussionRepository.save(discussion));
    }

    private ListResponse<DiscussionResponse> buildPagedTree(Page<Discussion> rootPage) {
        List<String> rootIds = rootPage.getContent().stream()
                .map(Discussion::getId)
                .toList();

        List<Discussion> allRelated = discussionRepository.findByRootIdIn(rootIds);
        
        List<Discussion> combined = new ArrayList<>(rootPage.getContent());
        combined.addAll(allRelated);

        List<DiscussionResponse> allResponses = combined.stream()
                .map(discussionMapper::entityToResponse)
                .toList();

        Map<String, List<DiscussionResponse>> groupedByParent = allResponses.stream()
                .filter(res -> res.getParentId() != null)
                .collect(Collectors.groupingBy(DiscussionResponse::getParentId));

        List<DiscussionResponse> rootResponses = allResponses.stream()
                .filter(res -> rootIds.contains(res.getId()))
                .toList();

        allResponses.forEach(res -> res.setReplies(groupedByParent.get(res.getId())));

        return ListResponse.of(rootResponses, rootPage);
    }

    @Override
    public void deleteDiscussion(String discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        // 🔐 Chốt chặn bảo mật: Chỉ cho phép chính chủ, ADMIN, hoặc Giáo viên sở hữu khóa học được phép xóa bài thảo luận
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Truy cứu Giáo viên sở hữu khóa học qua Shared Redis cực nhanh
        String courseOwnerKey = RedisPrefixConstants.getSharedCourseOwnerKey(discussion.getCourseId());
        String courseOwnerId = (String) redisService.get(courseOwnerKey);
        boolean isCourseOwner = courseOwnerId != null && courseOwnerId.equals(currentUserId);

        if (!discussion.getUserId().equals(currentUserId) && !isAdmin && !isCourseOwner) {
            throw new RuntimeException("Access denied: You are not authorized to delete this discussion!");
        }

        discussionRepository.deleteById(discussionId);
    }
}
