import { Router } from 'express';
import courseRoutes from './courseRoutes.js';
import reportRoutes from './reportRoutes.js';

const router = Router();

// Base Health check route
router.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'aggregator-service',
    timestamp: new Date().toISOString()
  });
});

// Register Course aggregation routes
router.use('/courses', courseRoutes);

// Register Admin Analytics and Report routes
router.use('/reports', reportRoutes);

export default router;
