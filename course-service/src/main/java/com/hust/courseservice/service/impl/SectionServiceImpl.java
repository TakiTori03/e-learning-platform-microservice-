package com.hust.courseservice.service.impl;


import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.Section;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final SectionMapper sectionMapper;

    @Override
    public SectionResponse create(SectionRequest request) {
        Section section = sectionMapper.requestToEntity(request);
        section = sectionRepository.save(section);
        return sectionMapper.entityToResponse(section);
    }

    @Override
    public List<SectionResponse> getByCourseId(String courseId) {
        return sectionMapper.entityToResponse(sectionRepository.findAllByCourseIdOrderByPositionAsc(courseId));
    }
}
