package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.enums.CourseAccess;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final LessonMapper lessonMapper;

    @Override
    public LessonResponse create(LessonRequest request) {
        validateCourseOwnership(request.getCourseId());
        Lesson lesson = lessonMapper.requestToEntity(request);
        lesson = lessonRepository.save(lesson);
        return lessonMapper.entityToResponse(lesson);
    }

    @Override
    public LessonResponse update(String id, LessonRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.LESSON,
                        AppConstants.Field_Constants.ID,
                        id));
        
        validateCourseOwnership(lesson.getCourseId());
        
        lessonMapper.partialUpdate(lesson, request);
        lesson = lessonRepository.save(lesson);
        return lessonMapper.entityToResponse(lesson);
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Lesson> lessons = lessonRepository.findAllById(ids);
        
        lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);
        
        lessonRepository.deleteAll(lessons);
    }

    @Override
    public LessonResponse detail(String id) {
        return lessonRepository.findById(id)
                .map(lessonMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.LESSON,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<LessonResponse> search(String text, Pageable pageable) {
        Page<Lesson> lessonPage;
        if (text == null || text.trim().isEmpty()) {
            lessonPage = lessonRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            lessonPage = lessonRepository.findAllBy(criteria, pageable);
        }
        return ListResponse.of(lessonMapper.entityToResponse(lessonPage.getContent()), lessonPage);
    }

    @Override
    public List<LessonResponse> getBySectionId(String sectionId) {
        return lessonMapper.entityToResponse(lessonRepository.findAllBySectionIdOrderByPositionAsc(sectionId));
    }

    @Override
    public List<LessonResponse> getBySectionIdEnrolled(String sectionId, String userId) {
       return List.of();
    }

    @Override
    public List<LessonResponse> getByCourseId(String courseId) {
        return lessonMapper.entityToResponse(lessonRepository.findAllByCourseIdOrderByPositionAsc(courseId));
    }

    @Override
    public List<LessonResponse> getFreeLessons(String courseId) {
        // Find lessons with FREE access for this course
        return lessonMapper.entityToResponse(lessonRepository.findAllByCourseIdAndAccessOrderByPositionAsc(courseId, CourseAccess.FREE));
    }

    @Override
    public void updateDone(String id, String userId) {

    }

    @Override
    public List<String> getUsersByLessonId(String id) {
        return List.of(); // Placeholder
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void reorder(List<String> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) return;

        List<Lesson> lessons = lessonRepository.findAllById(lessonIds);
        
        lessons.stream()
                .map(Lesson::getCourseId)
                .distinct()
                .forEach(this::validateCourseOwnership);

        java.util.Map<String, Lesson> lessonMap = lessons.stream()
                .collect(java.util.stream.Collectors.toMap(Lesson::getId, l -> l));

        List<Lesson> toUpdate = new java.util.ArrayList<>();
        for (int i = 0; i < lessonIds.size(); i++) {
            Lesson lesson = lessonMap.get(lessonIds.get(i));
            if (lesson != null) {
                lesson.setPosition(i + 1);
                toUpdate.add(lesson);
            }
        }

        if (!toUpdate.isEmpty()) {
            lessonRepository.saveAll(toUpdate);
        }
    }

    private void validateCourseOwnership(String courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) return;

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!course.getInstructorId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to perform this action on this course content!");
        }
    }
}
