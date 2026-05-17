import axios from "axios";
import { config } from "../config/index.js";
import { CourseMapper } from "../utils/mappers.js";
import {
  ApiResponse,
  CourseResponse,
  RatingResponse,
  ProgressResponse,
  InstructorResponse,
  ListResponse,
  EnrollmentResponse,
  WishlistResponse,
  CourseCardDTO,
  CourseDetailDTO,
  LearningCourseDTO,
} from "../types/index.js";

/**
 * Aggregates and enriches detailed course catalog details.
 */
export const getEnrichedCourseDetail = async (
  id: string,
  userId: string | null,
): Promise<CourseDetailDTO | null> => {
  const courseWithInstructorPromise = axios
    .get<ApiResponse<CourseResponse>>(
      `${config.gatewayUrl}/course/internal/courses/${id}`,
    )
    .then(async (r) => {
      const course = r.data?.payload || null;
      if (!course || !course.instructorId) return { course, instructor: null };

      const instructor = await axios
        .get<ApiResponse<InstructorResponse>>(
          `${config.gatewayUrl}/identity/internal/users/${course.instructorId}`,
        )
        .then((res) => res.data?.payload || null)
        .catch(() => null);

      return { course, instructor };
    })
    .catch(() => ({ course: null, instructor: null }));

  const ratingPromise = axios
    .post<ApiResponse<RatingResponse[]>>(
      `${config.gatewayUrl}/interaction/internal/reviews/course-ratings`,
      [id],
    )
    .then((r) => (r.data?.payload || []).find((i) => i.courseId === id) || null)
    .catch(() => null);

  const studentCountPromise = axios
    .get<ApiResponse<Record<string, number>>>(
      `${config.gatewayUrl}/order/internal/orders/enrollment-counts?courseIds=${id}`,
    )
    .then((r) => r.data?.payload || ({} as Record<string, number>))
    .then((payload) => payload[id] ?? 0)
    .catch(() => 0);

  const isBoughtPromise = userId
    ? axios
        .get<ApiResponse<boolean>>(
          `${config.gatewayUrl}/order/internal/orders/check-bought?userId=${userId}&courseId=${id}`,
        )
        .then((r) => r.data?.payload ?? false)
        .catch(() => false)
    : Promise.resolve(false);

  const progressPromise = userId
    ? axios
        .get<ApiResponse<ProgressResponse>>(
          `${config.gatewayUrl}/learning/internal/learning/progress?userId=${userId}&courseId=${id}`,
        )
        .then((r) => r.data?.payload?.progressPercentage ?? 0.0)
        .catch(() => 0.0)
    : Promise.resolve(0.0);

  const [courseResult, rating, studentCount, isBought, progress] =
    await Promise.all([
      courseWithInstructorPromise,
      ratingPromise,
      studentCountPromise,
      isBoughtPromise,
      progressPromise,
    ]);

  const { course, instructor } = courseResult;
  if (!course) return null;

  return CourseMapper.toDetail(course, instructor, rating, studentCount, isBought, progress);
};

/**
 * Searches and paginates using HIGH-PERFORMANCE Bulk Operations.
 * Uses strictly isolated CourseCardDTO mapping.
 */
export const searchEnrichedCourses = async (
  queryParams: Record<string, unknown>,
  userId?: string | null,
): Promise<ListResponse<CourseCardDTO> | null> => {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(queryParams)) {
    if (value !== undefined && value !== null && value !== "" && key !== "sort") {
      if (Array.isArray(value)) {
        value.forEach((v) => params.append(key, String(v)));
      } else {
        params.append(key, String(value));
      }
    }
  }

  // 1. Initial Catalog hit
  const courseSearchResponse = await axios
    .get<ApiResponse<ListResponse<CourseResponse>>>(
      `${config.gatewayUrl}/course/courses/search?${params.toString()}`
    )
    .then((r) => r.data?.payload)
    .catch(() => null);

  if (!courseSearchResponse) return null;

  const courses = courseSearchResponse.content || [];
  if (courses.length === 0) {
    return { ...courseSearchResponse, content: [] };
  }

  const courseIds = courses.map((c) => c.id);
  const instructorIds = Array.from(new Set(courses.map((c) => c.instructorId).filter(Boolean)));

  // 2. Trigger OPTIMIZED Bulk parallel fetches
  const ratingBulkPromise = axios
    .post<ApiResponse<RatingResponse[]>>(`${config.gatewayUrl}/interaction/internal/reviews/course-ratings`, courseIds)
    .then((r) => r.data?.payload || [])
    .catch(() => [] as RatingResponse[]);

  const studentCountBulkPromise = axios
    .get<ApiResponse<Record<string, number>>>(`${config.gatewayUrl}/order/internal/orders/enrollment-counts?courseIds=${courseIds.join(",")}`)
    .then((r) => r.data?.payload || ({} as Record<string, number>))
    .catch(() => ({}) as Record<string, number>);

  const instructorsBulkPromise = instructorIds.length 
    ? axios
        .post<ApiResponse<InstructorResponse[]>>(`${config.gatewayUrl}/identity/internal/users/bulk`, instructorIds)
        .then((r) => r.data?.payload || [])
        .catch(() => [] as InstructorResponse[])
    : Promise.resolve([] as InstructorResponse[]);

  const isBoughtBulkPromise = userId
    ? axios
        .get<ApiResponse<Record<string, boolean>>>(`${config.gatewayUrl}/order/internal/orders/check-bought-bulk?userId=${userId}&courseIds=${courseIds.join(",")}`)
        .then((r) => r.data?.payload || ({} as Record<string, boolean>))
        .catch(() => ({}) as Record<string, boolean>)
    : Promise.resolve({} as Record<string, boolean>);

  const progressBulkPromise = userId
    ? axios
        .get<ApiResponse<Record<string, any>>>(`${config.gatewayUrl}/learning/internal/learning/progress/bulk?userId=${userId}&courseIds=${courseIds.join(",")}`)
        .then((r) => r.data?.payload || ({} as Record<string, any>))
        .catch(() => ({}) as Record<string, any>)
    : Promise.resolve({} as Record<string, any>);

  // Execute collapsed round
  const [ratings, counts, instructors, boughtMapRaw, progressMapRaw] = await Promise.all([
    ratingBulkPromise,
    studentCountBulkPromise,
    instructorsBulkPromise,
    isBoughtBulkPromise,
    progressBulkPromise,
  ]);

  const ratingsMap = new Map(ratings.map((r) => [r.courseId, r]));
  const instructorsMap = new Map(instructors.map((i) => [i.id, i]));

  // 4. Final Mapping to dedicated compact card DTO
  const content = courses.map((course) => {
    const rating = ratingsMap.get(course.id) || null;
    const instructor = instructorsMap.get(course.instructorId || "") || null;
    const count = counts[course.id] ?? 0;
    const isBought = !!boughtMapRaw[course.id];
    const rawProgress = progressMapRaw[course.id];
    const progress = rawProgress?.progressPercentage ?? 0.0;

    return CourseMapper.toCard(course, instructor, rating, count, isBought, progress);
  });

  return {
    ...courseSearchResponse,
    content,
  };
};

/**
 * Personalized MyLearning retriever optimized via LearningCourseDTO.
 */
export const getMyLearningCourses = async (userId: string): Promise<LearningCourseDTO[]> => {
  const enrollments = await axios
    .get<ApiResponse<EnrollmentResponse[]>>(`${config.gatewayUrl}/learning/internal/enrollments?userId=${userId}`)
    .then((r) => r.data?.payload || [])
    .catch(() => [] as EnrollmentResponse[]);

  if (!enrollments.length) return [];

  const courseIds = enrollments.map((e) => e.courseId);
  
  const courseRawPromises = courseIds.map(id => 
    axios.get<ApiResponse<CourseResponse>>(`${config.gatewayUrl}/course/internal/courses/${id}`)
      .then(r => r.data?.payload || null)
      .catch(() => null)
  );

  const coursesList = await Promise.all(courseRawPromises);
  const validCourses = coursesList.filter((c): c is CourseResponse => c !== null);
  const instructorIds = Array.from(new Set(validCourses.map(c => c.instructorId).filter(Boolean)));

  const instructors = instructorIds.length
    ? await axios.post<ApiResponse<InstructorResponse[]>>(`${config.gatewayUrl}/identity/internal/users/bulk`, instructorIds)
        .then(r => r.data?.payload || [])
        .catch(() => [])
    : [];

  const instructorsMap = new Map(instructors.map(i => [i.id, i]));
  const coursesMap = new Map(validCourses.map(c => [c.id, c]));

  return enrollments.map((e) => {
    const course = coursesMap.get(e.courseId) || null;
    const instructor = instructorsMap.get(course?.instructorId || "") || null;
    
    // Standardized Clean Mapper
    return CourseMapper.toLearning(course, instructor, e);
  });
};

/**
 * Personalized Wishlist retrieving compact cards.
 */
export const getMyWishlistEnriched = async (userId: string): Promise<CourseCardDTO[]> => {
  const wishlist = await axios
    .get<ApiResponse<WishlistResponse[]>>(`${config.gatewayUrl}/interaction/wishlists/me`)
    .then((r) => r.data?.payload || [])
    .catch(() => [] as WishlistResponse[]);

  if (!wishlist.length) return [];

  const courseIds = wishlist.map((i) => i.courseId);

  const coursePromises = courseIds.map((id) =>
    axios.get<ApiResponse<CourseResponse>>(`${config.gatewayUrl}/course/internal/courses/${id}`)
      .then((r) => r.data?.payload || null)
      .catch(() => null)
  );

  const ratingBulkPromise = axios
    .post<ApiResponse<RatingResponse[]>>(`${config.gatewayUrl}/interaction/internal/reviews/course-ratings`, courseIds)
    .then(r => r.data?.payload || [])
    .catch(() => []);

  const countsBulkPromise = axios
    .get<ApiResponse<Record<string, number>>>(`${config.gatewayUrl}/order/internal/orders/enrollment-counts?courseIds=${courseIds.join(",")}`)
    .then(r => r.data?.payload || ({} as Record<string, number>))
    .catch(() => ({} as Record<string, number>));

  const [allCoursesRaw, ratings, counts] = await Promise.all([
    Promise.all(coursePromises),
    ratingBulkPromise,
    countsBulkPromise
  ]);

  const validCourses = allCoursesRaw.filter((c): c is CourseResponse => c !== null);
  if (!validCourses.length) return [];

  const instructorIds = Array.from(new Set(validCourses.map(c => c.instructorId).filter(Boolean)));

  const instructors = instructorIds.length
    ? await axios.post<ApiResponse<InstructorResponse[]>>(`${config.gatewayUrl}/identity/internal/users/bulk`, instructorIds)
        .then(r => r.data?.payload || [])
        .catch(() => [])
    : [];

  const ratingsMap = new Map(ratings.map(r => [r.courseId, r]));
  const instructorsMap = new Map(instructors.map(i => [i.id, i]));

  return validCourses.map((course) => {
    const rating = ratingsMap.get(course.id) || null;
    const instructor = instructorsMap.get(course.instructorId || "") || null;
    const count = counts[course.id] ?? 0;

    // Mapped cleanly to compact card
    return CourseMapper.toCard(course, instructor, rating, count, false, 0);
  });
};
