import { Response } from 'express';

export class ResponseHelper {
  static success<T>(res: Response, payload: T, statusCode = 200) {
    return res.status(statusCode).json({
      success: true,
      payload
    });
  }

  static error(res: Response, message: string, statusCode = 500) {
    return res.status(statusCode).json({
      success: false,
      message
    });
  }
}
