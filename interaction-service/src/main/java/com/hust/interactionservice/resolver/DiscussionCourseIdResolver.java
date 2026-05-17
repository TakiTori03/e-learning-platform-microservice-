package com.hust.interactionservice.resolver;

import com.hust.commonlibrary.resolver.CourseIdResolver;
import com.hust.interactionservice.entity.Discussion;
import com.hust.interactionservice.repository.DiscussionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tự động tra cứu courseId THẬT từ Discussion ID trong MongoDB.
 * Cung cấp dữ liệu sống cho Annotation phân quyền Aspect.
 */
@Component("discussionResolver")
@RequiredArgsConstructor
public class DiscussionCourseIdResolver implements CourseIdResolver {

    private final DiscussionRepository discussionRepository;

    @Override
    public String resolveCourseId(String domainId) {
        if (domainId == null || domainId.isBlank()) return null;

        return discussionRepository.findById(domainId)
                .map(Discussion::getCourseId)
                .orElse(null);
    }
}
