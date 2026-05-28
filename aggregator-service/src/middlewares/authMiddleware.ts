import { Response, NextFunction } from 'express';
import { authContext } from '../config/axiosSetup.js';
import { JWTPayload, EnhancedRequest } from '../types/index.js';

/**
 * Middleware to run asynchronous execution context capturing the client's Authorization header
 * and propagating it automatically to downstream Axios outbound requests.
 * Also resolves the current userId onto the request scope.
 */
export const requestContextMiddleware = (req: EnhancedRequest, res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization || '';
  req.userId = getUserIdFromToken(authHeader);
  req.userRole = getRoleFromToken(authHeader);
  authContext.run(authHeader, next);
};

/**
 * Safely decodes a JWT token to extract the unique userId (sub).
 * Does not require external libraries to minimize dependency overhead.
 */
export const getUserIdFromToken = (authHeader?: string): string | null => {
  if (!authHeader) return null;
  try {
    const token = authHeader.split(' ')[1];
    if (!token) return null;
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replaceAll('-', '+').replaceAll('_', '/');
    const jsonPayload = decodeURIComponent(
      Buffer.from(base64, 'base64')
        .toString()
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const payload = JSON.parse(jsonPayload) as JWTPayload;
    return payload.sub || payload.userId || null;
  } catch (error) {
    console.warn('BFF Warning: Failed to safely decode JWT token:', (error as Error).message);
    return null;
  }
};

/**
 * Safely decodes a JWT token to extract and clean the single business role.
 * Resolves to 'ADMIN', 'INSTRUCTOR', or 'STUDENT', filtering out Keycloak default roles.
 */
export const getRoleFromToken = (authHeader?: string): string | null => {
  if (!authHeader) return null;
  try {
    const token = authHeader.split(' ')[1];
    if (!token) return null;
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replaceAll('-', '+').replaceAll('_', '/');
    const jsonPayload = decodeURIComponent(
      Buffer.from(base64, 'base64')
        .toString()
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const payload = JSON.parse(jsonPayload);
    const roles: string[] = [];
    if (payload.roles && Array.isArray(payload.roles)) {
      roles.push(...payload.roles);
    }
    if (payload.realm_access?.roles && Array.isArray(payload.realm_access.roles)) {
      roles.push(...payload.realm_access.roles);
    }
    const upperRoles = new Set(roles.map(r => String(r).toUpperCase()));

    if (upperRoles.has("ADMIN")) return "ADMIN";
    if (upperRoles.has("INSTRUCTOR")) return "INSTRUCTOR";
    if (upperRoles.has("STUDENT")) return "STUDENT";
    
    return null;
  } catch (error) {
    console.warn('BFF Warning: Failed to safely decode JWT token role:', (error as Error).message);
    return null;
  }
};

/**
 * Middleware to restrict route access to specific roles.
 */
export const requireRole = (allowedRoles: string[]) => {
  return (req: EnhancedRequest, res: Response, next: NextFunction): void => {
    if (!req.userId) {
      res.status(401).json({ success: false, message: "Unauthorized: Token missing or invalid." });
      return;
    }
    if (!req.userRole || !allowedRoles.includes(req.userRole)) {
      res.status(403).json({ success: false, message: "Forbidden: Insufficient privileges." });
      return;
    }
    next();
  };
};


