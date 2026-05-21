package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.constant.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.utils.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import com.hust.commonlibrary.service.RedisService;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.ActionLog;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.enums.ActionLogType;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.entity.enums.FunctionType;
import com.hust.courseservice.mapper.CourseMapper;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.*;
import com.hust.courseservice.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.commonlibrary.annotation.CustomCacheEvict;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;
    private final LessonMapper lessonMapper;
    private final ActionLogRepository actionLogRepository;
    private final RedisService redisService;
    private final Random random = new Random();

    private void updateCourseOwnerCache(Course course) {
        try {
            String key = RedisPrefixConstants.getSharedCourseOwnerKey(course.getId());
            redisService.set(key, course.getInstructorId());
            log.info("Synced Redis: Course {} permanently mapped to Owner {}", course.getId(), course.getInstructorId());
        } catch (Exception e) {
            log.error("Cache Failure: Non-blocking exception syncing Redis for Course owner: ", e);
        }
    }

    private void deleteCourseOwnerCache(String courseId) {
        try {
            String key = RedisPrefixConstants.getSharedCourseOwnerKey(courseId);
            redisService.delete(key);
            log.info("Evicted Redis: Removed Course {} from Shared Auth", courseId);
        } catch (Exception e) {
            log.error("Cache Failure: Non-blocking exception evicting Course owner from Redis: ", e);
        }
    }

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
        
        // Permanently map courseId -> instructorId on Redis Cluster
        updateCourseOwnerCache(course);
        
        return courseMapper.entityToResponse(course);
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'course:detail:' + #id") // 🧹 DỌN DẸP CACHE: Xóa cache cũ ngay khi cập nhật thông tin khóa học!
    public CourseResponse update(String id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));

        validateCourseOwnership(course);

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
        // api update instroctor not allowed update instructor
        
        return courseMapper.entityToResponse(course);
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Course> courses = courseRepository.findAllById(ids);
        for (Course course : courses) {
            validateCourseOwnership(course);
        }
        courseRepository.deleteAll(courses);
        
        // Evict permanent mappings on deletion
        for (String id : ids) {
            deleteCourseOwnerCache(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @CustomCache(key = "'course:detail:' + #id", ttl = 30, unit = TimeUnit.MINUTES) // 🚀 TỐI ƯU HIỆU NĂNG: Cache toàn bộ Curriculum cực nặng trong 30 phút!
    public CourseResponse detail(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));
        
        CourseResponse response = courseMapper.entityToResponse(course);
        enrichCurriculum(response);
        return response;
    }



    private void enrichCurriculum(CourseResponse response) {
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
    public ListResponse<CourseResponse> search(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Pageable pageable
    ) {
        Query mongoQuery = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Search Query
        if (q != null && !q.trim().isEmpty()) {
            mongoQuery.addCriteria(TextCriteria.forDefaultLanguage().matching(q));
        }

        // 2. Authors
        if (authors != null && !authors.isEmpty()) {
            criteriaList.add(Criteria.where("instructorId").in(authors));
        }

        // 3. Topics (Category)
        if (topics != null && !topics.isEmpty()) {
            criteriaList.add(Criteria.where("category").in(
                    topics.stream().map(ObjectId::new).toList()
            ));
        }

        // 4. Levels
        if (levels != null && !levels.isEmpty()) {
            criteriaList.add(Criteria.where("level").in(levels));
        }

        // 5. Price
        if (prices != null && !prices.isEmpty()) {
            List<Criteria> priceCriteria = new ArrayList<>();
            for (String p : prices) {
                if ("Free".equalsIgnoreCase(p)) {
                    priceCriteria.add(Criteria.where("finalPrice").is(0.0));
                } else if ("Paid".equalsIgnoreCase(p)) {
                    priceCriteria.add(Criteria.where("finalPrice").gt(0.0));
                }
            }
            if (!priceCriteria.isEmpty()) {
                criteriaList.add(new Criteria().orOperator(priceCriteria.toArray(new Criteria[0])));
            }
        }

//        // 6. Status (Only show PUBLISHED to normal users, unless status is specified)
//        // Note: In a real scenario, you'd check roles here or have separate methods
//        // For now, let's add a condition if status is not explicitly requested
//        if (mongoQuery.getCriteria().stream().noneMatch(c -> c.getKey().equals("status"))) {
//            // criteriaList.add(Criteria.where("status").is(com.hust.courseservice.entity.enums.CourseStatus.PUBLISHED));
//            // Let's keep it flexible for now but aware of the need
//        }

        if (!criteriaList.isEmpty()) {
            mongoQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 6. Execution & Enrichment
        long totalRows = mongoTemplate.count(mongoQuery, Course.class);
        mongoQuery.with(pageable);

        List<Course> courses = mongoTemplate.find(mongoQuery, Course.class);
        List<CourseResponse> responses = courseMapper.entityToResponse(courses);
        Page<Course> coursePage = new PageImpl<>(courses, pageable, totalRows);
        return ListResponse.of(responses, coursePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getPopularCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("views").descending());
        return courseMapper.entityToResponse(courseRepository.findAll(pageable).getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getRelatedCourses(String courseId, int limit) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getCategory() == null) return Collections.emptyList();
        
        return courseMapper.entityToResponse(
                courseRepository.findAllByCategoryId(course.getCategory().getId())
                        .stream()
                        .filter(c -> !c.getId().equals(courseId))
                        .limit(limit)
                        .toList()
        );
    }

//    @Override
//    @Transactional(readOnly = true)
//    public CourseResponse getFullDetail(String id) {
//        return detail(id);
//    }

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
    @Transactional
    public void updateStatus(String id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> 
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));
        
        validateCourseOwnership(course);

        String oldStatus = course.getStatus().name();
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            course.setStatus(CourseStatus.ARCHIVED);
        } else {
            course.setStatus(CourseStatus.PUBLISHED);
        }
        courseRepository.save(course);

        // Log the action
        logAction(course.getId(), 
              ActionLogType.UPDATE,
                 "Course status updated from " + oldStatus + " to " + course.getStatus().name(),
                 FunctionType.COURSE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllActiveCourses() {
        return courseMapper.entityToResponse(courseRepository.findAllByStatus(CourseStatus.PUBLISHED)); 
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<Object> getHistories(String id, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<com.hust.courseservice.entity.ActionLog> logPage = actionLogRepository.findAllByCourseIdOrderByCreatedAtDesc(id, pageable);
        
        return ListResponse.of(new ArrayList<>(logPage.getContent()), logPage);
    }

    private void logAction(String courseId, 
                          ActionLogType type,
                          String description,
                          FunctionType functionType) {
        String userId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");
        
        ActionLog actionLog = ActionLog.builder()
                .courseId(courseId)
                .userId(userId)
                .type(type)
                .description(description)
                .functionType(functionType)
                .build();
        
        actionLogRepository.save(actionLog);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private void validateCourseOwnership(Course course) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!currentUserId.equals(course.getInstructorId())) {
            throw new AccessDeniedException("You do not have permission to perform this action on this course!");
        }
    }

    @Override
    @Transactional
    public CourseResponse adminCreate(CourseRequest request, String instructorId) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AppConstants.Resource_Constants.CATEGORY,
                            AppConstants.Field_Constants.ID,
                            request.getCategoryId()));
        }

        Course course = courseMapper.requestToEntity(request);
        course.setInstructorId(instructorId != null ? instructorId : SecurityUtils.getCurrentUserIdOrThrow());
        course.setCategory(category);
        course.setCourseSlug(toSlug(request.getName()) + "-" + random.nextInt(1000));
        course.setCode("CRS-" + System.currentTimeMillis() % 10000);

        course = courseRepository.save(course);
        
        // Synchronize permanent ownership mapping via Admin
        updateCourseOwnerCache(course);
        
        return courseMapper.entityToResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse adminUpdate(String id, CourseRequest request, String instructorId) {
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
        if (instructorId != null) {
            course.setInstructorId(instructorId);
        }
        if (request.getName() != null) {
            course.setCourseSlug(toSlug(request.getName()) + "-" + random.nextInt(1000));
        }

        course = courseRepository.save(course);
        
        // Synchronize permanent ownership mapping on Admin updates
        updateCourseOwnerCache(course);
        
        return courseMapper.entityToResponse(course);
    }

    @Override
    @Transactional
    public void adminDelete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        courseRepository.deleteAllById(ids);
        
        // Evict permanent mappings via Admin deletion
        for (String id : ids) {
            deleteCourseOwnerCache(id);
        }
    }

    @Override
    @Transactional
    public void adminUpdateStatus(String id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> 
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));
        
        String oldStatus = course.getStatus().name();
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            course.setStatus(CourseStatus.ARCHIVED);
        } else {
            course.setStatus(CourseStatus.PUBLISHED);
        }
        courseRepository.save(course);

        logAction(course.getId(), 
              ActionLogType.UPDATE,
                 "Course status updated from " + oldStatus + " to " + course.getStatus().name(),
                 FunctionType.COURSE);
    }
}
