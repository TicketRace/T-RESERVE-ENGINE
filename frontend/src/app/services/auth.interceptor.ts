import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const authService = inject(AuthService);
  const router = inject(Router);

  let clonedReq = req;
  if (token) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Если 401 и это не эндпоинты авторизации (чтобы не зациклить)
      if (error.status === 401 && !req.url.includes('/api/auth/login') && !req.url.includes('/api/auth/refresh')) {
        
        // Пробуем обновить токен
        if (localStorage.getItem('refreshToken')) {
          return authService.refresh().pipe(
            switchMap((res) => {
              // Если успешно обновили, повторяем оригинальный запрос
              const newReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${res.token}`
                }
              });
              return next(newReq);
            }),
            catchError((refreshErr) => {
              // Если рефреш провалился (например, тоже протух), разлогиниваем
              authService.logout();
              router.navigate(['/login'], { queryParams: { message: 'Ваша сессия истекла, пожалуйста, войдите снова' } });
              return throwError(() => refreshErr);
            })
          );
        } else {
          // Если рефреш токена нет вообще
          authService.logout();
          router.navigate(['/login'], { queryParams: { message: 'Ваша сессия истекла, пожалуйста, войдите снова' } });
        }
      }
      return throwError(() => error);
    })
  );
};
