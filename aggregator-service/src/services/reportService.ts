import axios from "axios";
import { config } from "../config/index.js";
import {
  ApiResponse,
  RevenueReportResponse,
  CourseSalesReportResponse,
  SignupReportResponse,
  CourseInsightReportResponse,
  AuthorCourseReportResponse,
  UserResponse,
  OrderResponse,
  CourseProgressReportResponse,
  SummaryReportResponse,
} from "../types/index.js";

/**
 * Orchestrates calls across Microservices to fetch Dashboard Analytics
 */

export const getSummary = async (): Promise<SummaryReportResponse> => {
  // Parallel fetch from multiple domains
  const [revenueRes, signupsRes, insightsRes] = await Promise.all([
    axios.get<ApiResponse<RevenueReportResponse>>(`${config.gatewayUrl}/order/admin/reports/revenues`).catch(() => null),
    axios.get<ApiResponse<SignupReportResponse>>(`${config.gatewayUrl}/identity/admin/reports/new-signups`).catch(() => null),
    axios.get<ApiResponse<CourseInsightReportResponse>>(`${config.gatewayUrl}/course/admin/reports/course-insights`).catch(() => null),
  ]);

  return {
    revenueData: revenueRes?.data?.payload || null,
    signupData: signupsRes?.data?.payload || null,
    courseData: insightsRes?.data?.payload || null,
  };
};

export const getCourseSales = async (): Promise<CourseSalesReportResponse[]> => {
  const res = await axios.get<ApiResponse<CourseSalesReportResponse[]>>(`${config.gatewayUrl}/order/admin/reports/course-sales`);
  return res.data?.payload || [];
};

export const getRevenues = async (startDate?: string, endDate?: string, groupBy?: string): Promise<RevenueReportResponse | null> => {
  const params = new URLSearchParams();
  if (startDate) params.append("startDate", startDate);
  if (endDate) params.append("endDate", endDate);
  if (groupBy) params.append("groupBy", groupBy);
  
  const res = await axios.get<ApiResponse<RevenueReportResponse>>(`${config.gatewayUrl}/order/admin/reports/revenues?${params.toString()}`);
  return res.data?.payload || null;
};

export const getNewSignups = async (startDate?: string, endDate?: string, groupBy?: string): Promise<SignupReportResponse | null> => {
  const params = new URLSearchParams();
  if (startDate) params.append("startDate", startDate);
  if (endDate) params.append("endDate", endDate);
  if (groupBy) params.append("groupBy", groupBy);

  const res = await axios.get<ApiResponse<SignupReportResponse>>(`${config.gatewayUrl}/identity/admin/reports/new-signups?${params.toString()}`);
  return res.data?.payload || null;
};

export const getUsersProgress = async (): Promise<CourseProgressReportResponse[]> => {
  const res = await axios.get<ApiResponse<CourseProgressReportResponse[]>>(`${config.gatewayUrl}/learning/admin/reports/users-progress`);
  return res.data?.payload || [];
};

export const getCourseInsights = async (): Promise<CourseInsightReportResponse | null> => {
  const res = await axios.get<ApiResponse<CourseInsightReportResponse>>(`${config.gatewayUrl}/course/admin/reports/course-insights`);
  return res.data?.payload || null;
};

export const getCoursesReportByAuthor = async (authorId?: string): Promise<AuthorCourseReportResponse | null> => {
  const url = authorId 
    ? `${config.gatewayUrl}/course/admin/reports/courses-by-author?authorId=${authorId}`
    : `${config.gatewayUrl}/course/admin/reports/courses-by-author`;
  const res = await axios.get<ApiResponse<AuthorCourseReportResponse>>(url);
  return res.data?.payload || null;
};

export const getTopUsers = async (limit: number = 10): Promise<UserResponse[]> => {
  const res = await axios.get<ApiResponse<UserResponse[]>>(`${config.gatewayUrl}/identity/admin/reports/top-users?limit=${limit}`);
  return res.data?.payload || [];
};

export const getTopOrders = async (limit: number = 10): Promise<OrderResponse[]> => {
  const res = await axios.get<ApiResponse<OrderResponse[]>>(`${config.gatewayUrl}/order/admin/reports/top-orders?limit=${limit}`);
  return res.data?.payload || [];
};
