package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final Random random = new Random();

    @Override
    public CourseResponse create(CourseRequest request) {
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
        course.setCourseSlug(toSlug(request.getName()) + "-" + random.nextInt(1000));
        course.setCode("CRS-" + System.currentTimeMillis() % 10000);

        course = courseRepository.save(course);
        return courseMapper.entityToResponse(course);
    }

    @Override
    public CourseResponse update(String id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AppConstants.Resource_Constants.CATEGORY,
                            AppConstants.Field_Constants.ID,
                            request.getCategoryId()));
        }

        courseMapper.partialUpdate(course, request);
        course.setCategory(category);
        if (request.getName() != null) {
            course.setCourseSlug(toSlug(request.getName()) + "-" + random.nextInt(1000));
        }

        course = courseRepository.save(course);
        return courseMapper.entityToResponse(course);
    }

    @Override
    public void delete(List<String> ids) {
        courseRepository.deleteAllById(ids);
    }

    @Override
    public CourseResponse detail(String id) {
        return courseRepository.findById(id)
                .map(courseMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<CourseResponse> search(String text, Pageable pageable) {
        Page<Course> coursePage;
        if (text == null || text.trim().isEmpty()) {
            coursePage = courseRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            coursePage = courseRepository.findAllBy(criteria, pageable);
        }

        return ListResponse.of(courseMapper.entityToResponse(coursePage.getContent()), coursePage);
    }

    @Override
    public List<CourseResponse> getPopularCourses(int limit) {
        // Logic from monolith: Aggregate orders to find most bought courses
        // Currently a placeholder returning newest courses
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return courseMapper.entityToResponse(courseRepository.findAll(pageable).getContent());
    }

    @Override
    public List<CourseResponse> getRelatedCourses(String courseId, int limit) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getCategory() == null) return List.of();
        
        // Find courses in same category excluding current one
        return courseMapper.entityToResponse(
                courseRepository.findAllByCategoryId(course.getCategory().getId())
                        .stream()
                        .filter(c -> !c.getId().equals(courseId))
                        .limit(limit)
                        .toList()
        );
    }

    @Override
    public List<CourseResponse> getSuggestedCourses(String userId, int limit) {
        // Monolith logic: find categories user has bought and suggest others
        return List.of(); // Placeholder
    }

    @Override
    public List<CourseResponse> getCoursesOrderedByUser(String userId) {
        // Monolith logic: call order helper
        return List.of(); // Placeholder
    }

    @Override
    public List<String> getWishlistIds(String userId) {
        return List.of(); // Placeholder
    }

    @Override
    public List<CourseResponse> getWishlistCourses(String userId) {
        return List.of(); // Placeholder
    }

    @Override
    public CourseResponse getEnrolledDetail(String id) {
        // Monolith logic: includes progress, sections, lessons
        return detail(id); 
    }

    @Override
    public CourseResponse getFullDetail(String id) {
        // Monolith logic: getCourseDetail
        return detail(id);
    }

    @Override
    public void increaseView(String id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            course.setViews(course.getViews() + 1);
            courseRepository.save(course);
        }
    }

    @Override
    public List<String> getUsersByCourseId(String id) {
        return List.of(); // Placeholder
    }

    @Override
    public void updateStatus(String id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> 
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));
        // Toggle status or set to specific one? Monolith uses isDeleted toggle.
        // Microservice uses CourseStatus enum.
        courseRepository.save(course);
    }

    @Override
    public List<CourseResponse> getAllActiveCourses() {
        // CourseStatus.PUBLISHED ? 
        return courseMapper.entityToResponse(courseRepository.findAll()); 
    }

    @Override
    public ListResponse<Object> getHistories(String id, int page, int limit) {
        return ListResponse.of(List.of(), page, limit, 0, 0, true); // Placeholder
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
