import { Response } from "express";
import { EnhancedRequest } from "../types/index.js";
import * as courseService from "../services/courseService.js";
import { catchAsync } from "../utils/catchAsync.js";
import { ResponseHelper } from "../utils/ResponseHelper.js";

/**
 * Aggregates and enriches coarse course catalog details with ratings, student counts,
 * personalized bought status, progress, and instructor profiles concurrently.
 */
export const getCourseDetailEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const { id } = req.params;
    const userId = req.userId || null;

    console.log(
      `BFF (Node.js): Fetching strongly-typed enriched course detail for ID: ${id} (User: ${userId || "GUEST"})`,
    );

    const enrichedCourse = await courseService.getEnrichedCourseDetail(
      id,
      userId,
    );

    if (!enrichedCourse) {
      return ResponseHelper.error(res, "Course not found", 404);
    }

    return ResponseHelper.success(res, enrichedCourse);
  }
);

/**
 * Searches, paginates and enriches a list of courses with ratings and student counts bulk concurrently.
 */
export const searchCoursesEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const userId = req.userId || null;

    console.log(
      `BFF (Node.js): Executing highly-optimized enriched course search for User: ${userId || "GUEST"} with query:`,
      req.query,
    );

    const enrichedSearch = await courseService.searchEnrichedCourses(
      req.query as Record<string, unknown>,
      userId,
    );

    if (!enrichedSearch) {
      return ResponseHelper.error(res, "Failed to retrieve course list from catalog service.", 500);
    }

    return ResponseHelper.success(res, enrichedSearch);
  }
);

/**
 * Retrieves personalized list of enrolled courses for the authenticated student,
 * enriched with catalog details and instructor profiles concurrently.
 */
export const getMyLearningCoursesEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const userId = req.userId;

    console.log(
      `BFF (Node.js): Fetching personalized "My Learning" courses for User: ${userId || "UNKNOWN"}`,
    );

    if (!userId) {
      return ResponseHelper.error(res, "Unauthorized: Missing or invalid token.", 401);
    }

    const courses = await courseService.getMyLearningCourses(userId);
    return ResponseHelper.success(res, courses);
  }
);

/**
 * Retrieves personalized list of wishlist courses for the authenticated student,
 * enriched with catalog details and instructor profiles concurrently.
 */
export const getMyWishlistCoursesEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const userId = req.userId;

    console.log(
      `BFF (Node.js): Fetching personalized enriched "Wishlist" courses for User: ${userId || "UNKNOWN"}`,
    );

    if (!userId) {
      return ResponseHelper.error(res, "Unauthorized: Missing or invalid token.", 401);
    }

    const courses = await courseService.getMyWishlistEnriched(userId);
    return ResponseHelper.success(res, courses);
  }
);

/**
 * Searches and paginates courses for the authenticated instructor (only their own courses).
 */
export const getInstructorCoursesEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const userId = req.userId;
    const userRole = req.userRole;

    console.log(
      `BFF (Node.js): Executing instructor course search for User: ${userId || "UNKNOWN"} (Role: ${userRole})`,
    );

    if (!userId || userRole !== "INSTRUCTOR") {
      return ResponseHelper.error(res, "Access denied: Instructor role required.", 403);
    }

    const query = { ...req.query };

    const enrichedSearch = await courseService.searchEnrichedCourses(
      query as Record<string, unknown>,
      userId,
      "/course/courses/instructor/search"
    );

    if (!enrichedSearch) {
      return ResponseHelper.error(res, "Failed to retrieve instructor courses.", 500);
    }

    return ResponseHelper.success(res, enrichedSearch);
  }
);

/**
 * Searches and paginates all system courses for the authenticated Admin.
 */
export const getAdminCoursesEnriched = catchAsync(
  async (req: EnhancedRequest, res: Response) => {
    const userId = req.userId;
    const userRole = req.userRole;

    console.log(
      `BFF (Node.js): Executing admin course search for User: ${userId || "UNKNOWN"} (Role: ${userRole})`,
    );

    if (!userId || userRole !== "ADMIN") {
      return ResponseHelper.error(res, "Access denied: Admin role required.", 403);
    }

    const query = { ...req.query };

    const enrichedSearch = await courseService.searchEnrichedCourses(
      query as Record<string, unknown>,
      userId,
      "/course/courses/admin/search"
    );

    if (!enrichedSearch) {
      return ResponseHelper.error(res, "Failed to retrieve admin courses.", 500);
    }

    return ResponseHelper.success(res, enrichedSearch);
  }
);
