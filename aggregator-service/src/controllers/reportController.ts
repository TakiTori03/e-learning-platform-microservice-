import { Response } from "express";
import { EnhancedRequest } from "../types/index.js";
import * as reportService from "../services/reportService.js";
import { catchAsync } from "../utils/catchAsync.js";
import { ResponseHelper } from "../utils/ResponseHelper.js";

export const getSummary = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const summary = await reportService.getSummary();
  return ResponseHelper.success(res, summary);
});

export const getCourseSales = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const data = await reportService.getCourseSales();
  return ResponseHelper.success(res, data);
});

export const getRevenues = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const { startDate, endDate, groupBy } = req.query;
  const data = await reportService.getRevenues(
    startDate as string, 
    endDate as string, 
    groupBy as string
  );
  return ResponseHelper.success(res, data);
});

export const getNewSignups = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const { startDate, endDate, groupBy } = req.query;
  const data = await reportService.getNewSignups(
    startDate as string, 
    endDate as string, 
    groupBy as string
  );
  return ResponseHelper.success(res, data);
});

export const getUsersProgress = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const data = await reportService.getUsersProgress();
  return ResponseHelper.success(res, data);
});

export const getCourseInsights = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const data = await reportService.getCourseInsights();
  return ResponseHelper.success(res, data);
});

export const getCoursesReportByAuthor = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const userId = req.userId;
  const userRole = req.userRole;

  if (!userId) {
    return ResponseHelper.error(res, "Unauthorized: Missing token.", 401);
  }

  let targetAuthorId = req.query.authorId as string;

  if (userRole === "INSTRUCTOR") {
    targetAuthorId = userId;
  } else if (userRole !== "ADMIN") {
    return ResponseHelper.error(res, "Access denied: Admin or Instructor role required.", 403);
  }

  const data = await reportService.getCoursesReportByAuthor(targetAuthorId);
  return ResponseHelper.success(res, data);
});

export const getTopUsers = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const limit = req.query.limit ? parseInt(req.query.limit as string) : 10;
  const data = await reportService.getTopUsers(limit);
  return ResponseHelper.success(res, data);
});

export const getTopOrders = catchAsync(async (req: EnhancedRequest, res: Response) => {
  const limit = req.query.limit ? parseInt(req.query.limit as string) : 10;
  const data = await reportService.getTopOrders(limit);
  return ResponseHelper.success(res, data);
});
