package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.courseservice.client.InteractionClient;
import com.hust.courseservice.client.LearningClient;
import com.hust.courseservice.client.OrderClient;
import com.hust.courseservice.client.UserClient;
import com.hust.courseservice.client.dto.UserInternalResponse;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.mapper.CourseMapper;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;
    private final LessonMapper lessonMapper;
    private final UserClient userClient;
    private final InteractionClient interactionClient;
    private final OrderClient orderClient;
    private final LearningClient learningClient;
    private final Random random = new Random();

    @Override
    @Transactional
    public CourseResponse create(CourseRequest request) {
        String instructorId = SecurityUtils.getCurrentUserIdOrThrow();

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
    @Transactional
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
    @Transactional
    public void delete(List<String> ids) {
        courseRepository.deleteAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse detail(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));
        
        CourseResponse response = courseMapper.entityToResponse(course);
        List<CourseResponse> responses = Collections.singletonList(response);
        
        enrichInstructors(responses);
        enrichRatings(responses);
        enrichEnrollments(responses);

        // Handle User Context & Progress
        String currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Set<String> finishedLessonIds = new HashSet<>();
        
        if (currentUserId != null) {
            // Check Ownership
            ApiResponse<Boolean> isBoughtRes = orderClient.checkIfBought(currentUserId, id);
            if (isBoughtRes != null && isBoughtRes.isSuccess()) {
                response.setIsBought(isBoughtRes.getPayload());
            }

            // Fetch Progress
            try {
                ApiResponse<LearningClient.CourseProgressInternalResponse> progressRes = 
                        learningClient.getCourseProgress(currentUserId, id);
                if (progressRes != null && progressRes.isSuccess() && progressRes.getPayload() != null) {
                    var payload = progressRes.getPayload();
                    response.setProgress(payload.getProgressPercentage());
                    if (payload.getFinishedLessonIds() != null) {
                        finishedLessonIds.addAll(payload.getFinishedLessonIds());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch learning progress: {}", e.getMessage());
            }
        }

        enrichCurriculum(response, finishedLessonIds);
        return response;
    }

    private void enrichInstructors(List<CourseResponse> responses) {
        List<String> instructorIds = responses.stream()
                .map(CourseResponse::getInstructorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        if (instructorIds.isEmpty()) return;

        try {
            ApiResponse<List<UserInternalResponse>> userBulkResponse = userClient.getUsersByIds(instructorIds);
            if (userBulkResponse != null && userBulkResponse.isSuccess() && userBulkResponse.getPayload() != null) {
                Map<String, UserInternalResponse> userMap = userBulkResponse.getPayload().stream()
                        .collect(Collectors.toMap(UserInternalResponse::getId, u -> u));
                
                responses.forEach(resp -> {
                    if (resp.getInstructorId() != null) {
                        resp.setInstructor(userMap.get(resp.getInstructorId()));
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to bulk fetch instructors: {}", e.getMessage());
        }
    }

    private void enrichRatings(List<CourseResponse> responses) {
        List<String> courseIds = responses.stream()
                .map(CourseResponse::getId)
                .toList();
        
        if (courseIds.isEmpty()) return;

        try {
            ApiResponse<List<InteractionClient.InternalCourseRatingResponse>> ratingBulkResponse = 
                    interactionClient.getCourseRatingsBulk(courseIds);
            
            if (ratingBulkResponse != null && ratingBulkResponse.isSuccess() && ratingBulkResponse.getPayload() != null) {
                Map<String, InteractionClient.InternalCourseRatingResponse> ratingMap = ratingBulkResponse.getPayload().stream()
                        .collect(Collectors.toMap(InteractionClient.InternalCourseRatingResponse::getCourseId, r -> r));
                
                responses.forEach(resp -> {
                    InteractionClient.InternalCourseRatingResponse rating = ratingMap.get(resp.getId());
                    if (rating != null) {
                        resp.setAvgRatingStars(rating.getAvgRatingStars());
                        resp.setNumOfReviews(rating.getNumOfReviews().intValue());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to bulk fetch ratings: {}", e.getMessage());
        }
    }

    private void enrichEnrollments(List<CourseResponse> responses) {
        List<String> courseIds = responses.stream()
                .map(CourseResponse::getId)
                .toList();
        
        if (courseIds.isEmpty()) return;

        try {
            ApiResponse<Map<String, Long>> enrollmentRes = orderClient.getEnrollmentCountsBulk(courseIds);
            if (enrollmentRes != null && enrollmentRes.isSuccess() && enrollmentRes.getPayload() != null) {
                Map<String, Long> countMap = enrollmentRes.getPayload();
                responses.forEach(resp -> {
                    Long count = countMap.get(resp.getId());
                    resp.setStudentCount(count != null ? count.intValue() : 0);
                });
            }
        } catch (Exception e) {
            log.error("Failed to bulk fetch student counts: {}", e.getMessage());
        }
    }

    private void enrichCurriculum(CourseResponse response, Set<String> finishedLessonIds) {
        var sections = sectionRepository.findAllByCourseIdOrderByPositionAsc(response.getId());
        var allLessons = lessonRepository.findAllByCourseIdOrderByPositionAsc(response.getId());

        var lessonsBySection = allLessons.stream()
                .collect(Collectors.groupingBy(Lesson::getSectionId));

        int totalLessons = allLessons.size();
        double totalDuration = allLessons.stream()
                .mapToDouble(l -> l.getVideoLength() != null ? l.getVideoLength() : 0)
                .sum();

        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    var sResp = sectionMapper.entityToResponse(section);
                    var lessons = lessonsBySection.getOrDefault(section.getId(), Collections.emptyList());
                    
                    List<LessonResponse> lResponses = lessonMapper.entityToResponse(lessons);
                    // Standard: Overlay "isDone" from learning-service
                    lResponses.forEach(lr -> {
                        if (finishedLessonIds.contains(lr.getId())) {
                            lr.setIsDone(true);
                        }
                    });
                    
                    sResp.setLessons(lResponses);
                    return sResp;
                })
                .toList();

        response.setSections(sectionResponses);
        response.setSectionCount(sections.size());
        response.setLessonCount(totalLessons);
        response.setTotalVideosLength(totalDuration);
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<CourseResponse> search(String text, Pageable pageable) {
        Page<Course> coursePage;
        if (text == null || text.trim().isEmpty()) {
            coursePage = courseRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            coursePage = courseRepository.findAllBy(criteria, pageable);
        }

        List<CourseResponse> responses = courseMapper.entityToResponse(coursePage.getContent());
        enrichInstructors(responses);
        enrichRatings(responses);
        enrichEnrollments(responses);
        return ListResponse.of(responses, coursePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getPopularCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("views").descending());
        List<CourseResponse> responses = courseMapper.entityToResponse(courseRepository.findAll(pageable).getContent());
        enrichInstructors(responses);
        enrichRatings(responses);
        enrichEnrollments(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getRelatedCourses(String courseId, int limit) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getCategory() == null) return Collections.emptyList();
        
        List<CourseResponse> responses = courseMapper.entityToResponse(
                courseRepository.findAllByCategoryId(course.getCategory().getId())
                        .stream()
                        .filter(c -> !c.getId().equals(courseId))
                        .limit(limit)
                        .toList()
        );
        enrichInstructors(responses);
        enrichRatings(responses);
        enrichEnrollments(responses);
        return responses;
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
        return Collections.emptyList();
    }

    @Override
    public List<CourseResponse> getWishlistCourses(String userId) {
        return List.of(); // Placeholder
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getEnrolledDetail(String id) {
        // Monolith logic: includes progress, sections, lessons
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getFullDetail(String id) {
        // Monolith logic: getCourseDetail
        return null;
    }

    @Override
    @Transactional
    public void increaseView(String id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            course.setViews(course.getViews() + 1);
            courseRepository.save(course);
        }
    }

    @Override
    public List<String> getUsersByCourseId(String id) {
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void updateStatus(String id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> 
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));
        // Toggle status or set to specific one? Monolith uses isDeleted toggle.
        // Microservice uses CourseStatus enum.
        courseRepository.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllActiveCourses() {
        // CourseStatus.PUBLISHED ? 
        return courseMapper.entityToResponse(courseRepository.findAll()); 
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<Object> getHistories(String id, int page, int limit) {
        return ListResponse.of(Collections.emptyList(), page, limit, 0, 0, true);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
