package com.hust.courseservice.service.impl;


import com.hust.courseservice.dto.request.LessonRequest;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.service.LessonService;
import lombok.RequiredArgsConstructor;
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
    public List<LessonResponse> getBySectionId(String sectionId) {
        return lessonMapper.entityToResponse(lessonRepository.findAllBySectionIdOrderByPositionAsc(sectionId));
    }
}
