import { Router } from "express";
import * as reportController from "../controllers/reportController.js";

const router = Router();

// Toàn bộ route này nên được bọc bởi Middleware kiểm tra quyền ADMIN (isAuth, isAdmin)
// Hiện tại BFF giả định các downstream service sẽ check hoặc API Gateway check.
// Nhưng tốt nhất BFF cũng nên có role guard.

router.get("/summary", reportController.getSummary);
router.get("/course-sales", reportController.getCourseSales);
router.get("/revenues", reportController.getRevenues);
router.get("/new-signups", reportController.getNewSignups);
router.get("/users-progress", reportController.getUsersProgress);
router.get("/course-insights", reportController.getCourseInsights);
router.get(
  "/courses-report-by-author",
  reportController.getCoursesReportByAuthor,
);
router.get("/get-top-users", reportController.getTopUsers);
router.get("/get-top-orders", reportController.getTopOrders);

export default router;
