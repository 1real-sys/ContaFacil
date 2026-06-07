import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { StorageService } from '../services/storage.service';

const PUBLIC_ROUTES = ['/login', '/register'];
const AUTH_API = ['/api/auth/login', '/api/auth/register'];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const storage = inject(StorageService);
  const router = inject(Router);
  const token = storage.getToken();

  const isAuthEndpoint = AUTH_API.some((p) => req.url.includes(p));

  if (token && !isAuthEndpoint) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const url = router.url.split('?')[0];

      if (error.status === 401 && !PUBLIC_ROUTES.includes(url)) {
        storage.clear();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
