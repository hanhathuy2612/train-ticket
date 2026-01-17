import { HttpInterceptorFn } from '@angular/common/http';

export const userIdInterceptor: HttpInterceptorFn = (req, next) => {
  // Add X-User-Id header for ticket booking endpoints
  if (req.url.includes('/api/tickets/')) {
    const modifiedReq = req.clone({
      setHeaders: {
        'X-User-Id': '1' // Guest user ID
      }
    });
    return next(modifiedReq);
  }
  return next(req);
};
