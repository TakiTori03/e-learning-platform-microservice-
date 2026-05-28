import { Router } from "express";
import * as reportController from "../controllers/reportController.js";
import { requireRole } from "../middlewares/authMiddleware.js";

const router = Router();

// Secure all endpoints with role guards at the BFF layer
router.get("/summary", requireRole(["ADMIN"]), reportController.getSummary);
router.get("/course-sales", requireRole(["ADMIN"]), reportController.getCourseSales);
router.get("/revenues", requireRole(["ADMIN"]), reportController.getRevenues);
router.get("/new-signups", requireRole(["ADMIN"]), reportController.getNewSignups);
router.get("/users-progress", requireRole(["ADMIN"]), reportController.getUsersProgress);
router.get("/course-insights", requireRole(["ADMIN"]), reportController.getCourseInsights);
router.get("/get-top-users", requireRole(["ADMIN"]), reportController.getTopUsers);
router.get("/get-top-orders", requireRole(["ADMIN"]), reportController.getTopOrders);

// Author reports are accessible by both ADMIN and INSTRUCTOR
router.get(
  "/courses-report-by-author",
  requireRole(["ADMIN", "INSTRUCTOR"]),
  reportController.getCoursesReportByAuthor,
);

export default router;
