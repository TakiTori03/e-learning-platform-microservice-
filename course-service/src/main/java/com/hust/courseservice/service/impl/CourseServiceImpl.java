package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.mapper.CourseMapper;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    @Override
    public CourseResponse createCourse(CourseRequest request) {
        String instructorId = SecurityContextHolder.getContext().getAuthentication().getName();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AppConstants.Resource_Constants.CATEGORY,
                            AppConstants.Field_Constants.ID,
                            request.getCategoryId()));
        }

        Course course = courseMapper.requestToEntity(request);
        course.setInstructorId(instructorId);
        course.setCategory(category);
        course.setCourseSlug(toSlug(request.getName()) + "-" + new Random().nextInt(1000));
        course.setCode("CRS-" + System.currentTimeMillis() % 10000);

        course = courseRepository.save(course);
        return courseMapper.entityToResponse(course);
    }

    @Override
    public List<CourseResponse> getAllCourses() {
        return courseMapper.entityToResponse(courseRepository.findAll());
    }

    @Override
    public List<CourseResponse> searchCourses(String query) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
                .matching(query);
        return courseMapper.entityToResponse(courseRepository.findAllBy(criteria));
    }

    @Override
    public List<CourseResponse> getCoursesByCategory(String categoryId) {
        return courseMapper.entityToResponse(courseRepository.findAllByCategoryId(categoryId));
    }

    @Override
    public CourseResponse getCourseById(String id) {
        return courseRepository.findById(id)
                .map(courseMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
