import { Request } from 'express';

// --- Common Interfaces ---
export interface EnhancedRequest extends Request {
  userId?: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  payload: T;
}

export interface ListResponse<T> {
  content: T[];
  pageNumber?: number;
  pageSize?: number;
  totalElements?: number;
  totalPages?: number;
  last?: boolean;
  [key: string]: unknown;
}

export interface JWTPayload {
  sub?: string;
  userId?: string;
  [key: string]: unknown;
}

// --- Course & Catalog Interfaces ---

export interface CourseCategory {
  id: string;
  name: string;
  categorySlug?: string;
  icon?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CourseResponse {
  id: string;
  name?: string;
  code?: string;
  subTitle?: string;
  thumbnail?: string;
  access?: string;
  coursePreview?: string;
  views?: number;
  price?: number;
  finalPrice?: number;
  description?: string;
  level?: string;
  courseSlug?: string;
  status?: string;
  instructorId?: string;
  category?: CourseCategory | null;
  sections?: unknown[] | null;
  requirements?: string[];
  willLearns?: string[];
  tags?: string[];
  sectionCount?: number | null;
  lessonCount?: number | null;
  totalVideosLength?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

// --- Interaction / Review Interfaces ---

export interface RatingResponse {
  courseId: string;
  avgRatingStars: number;
  numOfReviews: number;
}

// --- Instructor / Identity Interfaces ---

export interface InstructorResponse {
  id: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  avatar?: string;
}

// --- Learning & Progress Interfaces ---

export interface ProgressResponse {
  progressPercentage?: number;
  [key: string]: unknown;
}

// --- Optimized DTOs (API Contracts) ---

/**
 * DTO designed purely for compact grid/list card rendering.
 * Minimizes overhead for aggregate searches.
 */
export interface CourseCardDTO {
  id: string;
  name: string;
  subTitle: string;
  thumbnail: string;
  courseSlug: string;
  price: number;
  finalPrice: number;
  access: string;
  level: string;
  instructorId: string;
  instructor: {
    id: string;
    firstName?: string;
    lastName?: string;
    avatar?: string;
  } | null;
  avgRatingStars: number;
  numOfReviews: number;
  studentCount: number;
  isBought: boolean;
  progress: number;
  category: { id: string; name: string } | null;
}

/**
 * Full Detail Payload intended for comprehensive single item viewer.
 */
export interface CourseDetailDTO extends CourseResponse {
  instructor: InstructorResponse | null;
  avgRatingStars: number;
  numOfReviews: number;
  studentCount: number;
  isBought: boolean;
  progress: number;
}

/**
 * DTO strictly refined for monitoring enrolled status and ongoing consumption.
 */
export interface LearningCourseDTO {
  id: string;
  name: string;
  thumbnail: string;
  instructor: {
    id: string;
    firstName?: string;
    lastName?: string;
    avatar?: string;
  } | null;
  progress: number;
  isCompleted: boolean;
  completedAt: string | null;
  lessonCount: number | null;
  totalVideosLength: number | null;
  lastAccessedLessonId?: string | null;
}

export interface EnrollmentResponse {
  userId: string;
  courseId: string;
  orderId?: string;
  progress: number;
  isCompleted: boolean;
  completedAt?: string;
  lastAccessedLessonId?: string;
}

export interface WishlistResponse {
  id: string;
  courseId: string;
  userId: string;
}

// --- Dashboard Reporting Interfaces ---

export interface RevenueDataPoint {
  period: string;
  revenue: number;
}

export interface RevenueReportResponse {
  totalRevenue: number;
  currency: string;
  dataPoints: RevenueDataPoint[];
}

export interface CourseSalesReportResponse {
  courseId: string;
  courseName: string;
  totalSales: number;
  totalRevenue: number;
}

export interface SignupDataPoint {
  period: string;
  count: number;
}

export interface SignupReportResponse {
  totalSignups: number;
  dataPoints: SignupDataPoint[];
}

export interface CourseInsightReportResponse {
  totalCourses: number;
  totalActiveCourses: number;
  totalDraftCourses: number;
  averageRating: number;
  totalReviews: number;
  totalViews: number;
}

export interface AuthorCourseReportResponse {
  instructorId: string;
  totalCourses: number;
  totalViews: number;
  averageRating: number;
  totalReviews: number;
  studentCount: number;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar: string;
  status: string;
  lastLogin: string;
  createdAt: string;
}

export interface OrderResponse {
  id: string;
  userId: string;
  totalPrice: number;
  status: string;
  createdAt: string;
}

export interface CourseProgressReportResponse {
  courseId: string;
  averageProgress: number;
  totalEnrollments: number;
  completedEnrollments: number;
}

export interface SummaryReportResponse {
  revenueData: RevenueReportResponse | null;
  signupData: SignupReportResponse | null;
  courseData: CourseInsightReportResponse | null;
}
