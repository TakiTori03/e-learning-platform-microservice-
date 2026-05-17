import { Request, Response, NextFunction } from 'express';
import { ResponseHelper } from '../utils/ResponseHelper.js';

export interface CustomError extends Error {
  statusCode?: number;
}

export const errorHandlerMiddleware = (
  err: CustomError,
  req: Request,
  res: Response,
  next: NextFunction
): void => {
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  console.error(`[BFF Error] ${req.method} ${req.url} - Status: ${statusCode} - Message: ${message}`);
  if (err.stack && process.env.NODE_ENV !== 'production') {
    console.error(err.stack);
  }

  ResponseHelper.error(res, message, statusCode);
};
