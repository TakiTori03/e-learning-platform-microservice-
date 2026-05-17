import { Router } from 'express';
import { 
  getCourseDetailEnriched, 
  searchCoursesEnriched,
  getMyLearningCoursesEnriched,
  getMyWishlistCoursesEnriched 
} from '../controllers/courseController.js';

const router = Router();

router.get('/search', searchCoursesEnriched);
router.get('/my-learning', getMyLearningCoursesEnriched);
router.get('/wishlist', getMyWishlistCoursesEnriched);
router.get('/detail/:id', getCourseDetailEnriched);

export default router;
