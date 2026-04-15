package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.SectionRequest;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.Section;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
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
    public SectionResponse update(String id, SectionRequest request) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.SESSION,
                        AppConstants.Field_Constants.ID,
                        id));
        sectionMapper.partialUpdate(section, request);
        section = sectionRepository.save(section);
        return sectionMapper.entityToResponse(section);
    }

    @Override
    public void delete(String id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.SESSION,
                        AppConstants.Field_Constants.ID,
                        id));
        sectionRepository.delete(section);
    }

    @Override
    public SectionResponse detail(String id) {
        return sectionRepository.findById(id)
                .map(sectionMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.SESSION,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<SectionResponse> search(String text, Pageable pageable) {
        Page<Section> sectionPage;
        if (text == null || text.trim().isEmpty()) {
            sectionPage = sectionRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            sectionPage = sectionRepository.findAllBy(criteria, pageable);
        }
        return ListResponse.of(sectionMapper.entityToResponse(sectionPage.getContent()), sectionPage);
    }

    @Override
    public List<SectionResponse> getByCourseId(String courseId) {
        return sectionMapper.entityToResponse(sectionRepository.findAllByCourseIdOrderByPositionAsc(courseId));
    }
}
