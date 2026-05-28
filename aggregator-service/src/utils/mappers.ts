import {
  CourseResponse,
  InstructorResponse,
  CourseCardDTO,
  CourseDetailDTO,
  LearningCourseDTO,
  EnrollmentResponse
} from "../types/index.js";

export class CourseMapper {
  /**
   * MAPS TO FULL DETAIL
   */
  static toDetail(
    course: CourseResponse,
    instructor: InstructorResponse | null,
    isBought: boolean,
    progress: number
  ): CourseDetailDTO {
    return {
      ...course,
      instructor,
      studentCount: course.studentCount ?? 0,
      isBought,
      progress,
      avgRatingStars: course.avgRatingStars ?? 0.0,
      numOfReviews: course.numOfReviews ?? 0,
    };
  }

  /**
   * MAPS TO COMPACT SEARCH CARD
   * Excludes non-essential dense properties to reduce bandwidth bloat.
   */
  static toCard(
    course: CourseResponse,
    instructor: InstructorResponse | null,
    isBought: boolean,
    progress: number
  ): CourseCardDTO {
    return {
      id: course.id,
      name: course.name || "",
      subTitle: course.subTitle || "",
      thumbnail: course.thumbnail || "",
      courseSlug: course.courseSlug || "",
      price: course.price ?? 0,
      finalPrice: course.finalPrice ?? course.price ?? 0,
      access: course.access || "FREE",
      level: course.level || "ALL",
      status: course.status,
      instructorId: course.instructorId || "",
      instructor: instructor
        ? {
            id: instructor.id,
            firstName: instructor.firstName,
            lastName: instructor.lastName,
            avatar: instructor.avatar,
          }
        : null,
      category: course.category
        ? { id: course.category.id, name: course.category.name }
        : null,
      studentCount: course.studentCount ?? 0,
      avgRatingStars: course.avgRatingStars ?? 0.0,
      numOfReviews: course.numOfReviews ?? 0,
      isBought,
      progress,
    };
  }

  /**
   * MAPS TO LEARNING GRID ELEMENT
   * Strips commercial details, isolates ongoing progress status.
   */
  static toLearning(
    course: CourseResponse | null,
    instructor: InstructorResponse | null,
    enrollment: EnrollmentResponse
  ): LearningCourseDTO {
    return {
      id: enrollment.courseId,
      name: course?.name || "",
      thumbnail: course?.thumbnail || "",
      instructor: instructor
        ? {
            id: instructor.id,
            firstName: instructor.firstName,
            lastName: instructor.lastName,
            avatar: instructor.avatar,
          }
        : null,
      progress: enrollment.progress ?? 0.0,
      isCompleted: enrollment.isCompleted ?? false,
      completedAt: enrollment.completedAt ?? null,
      lessonCount: course?.lessonCount ?? null,
      totalVideosLength: course?.totalVideosLength ?? null,
      lastAccessedLessonId: enrollment.lastAccessedLessonId ?? null,
    };
  }
}
