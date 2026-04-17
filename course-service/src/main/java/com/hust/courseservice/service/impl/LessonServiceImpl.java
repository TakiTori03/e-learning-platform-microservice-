package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.enums.CourseAccess;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;

    @Override
    public LessonResponse create(LessonRequest request) {
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
        lessonMapper.partialUpdate(lesson, request);
        lesson = lessonRepository.save(lesson);
        return lessonMapper.entityToResponse(lesson);
    }

    @Override
    public void delete(String id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.LESSON,
                        AppConstants.Field_Constants.ID,
                        id));
        lessonRepository.delete(lesson);
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
}
