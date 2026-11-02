import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { API_URL } from '../http/api-url';
import { rutaLogin } from './rol';
import { Sesion } from './sesion';

/**
 * Agrega el access token a cada pedido al backend. Si el backend responde 401 (token vencido),
 * renueva el token y repite el pedido. Si tampoco se puede renovar, manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  const token = sesion.accessToken();

  // El token sólo va a nuestra API, y no a /auth: esos endpoints son públicos, y si el refresh
  // pasara por acá, un 401 del refresh dispararía otro refresh.
  if (!token || !req.url.startsWith(API_URL) || req.url.startsWith(`${API_URL}/auth/`)) {
    return next(req);
  }

  return next(conToken(req, token)).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      // Otro pedido ya renovó el token mientras este viajaba: se repite con el nuevo.
      const actual = sesion.accessToken();
      if (actual && actual !== token) {
        return next(conToken(req, actual));
      }

      const rol = sesion.rol();
      return sesion.refrescar().pipe(
        catchError(() => {
          if (rol) {
            void router.navigate(rutaLogin(rol));
          }
          return throwError(() => error);
        }),
        switchMap((nuevo) => next(conToken(req, nuevo))),
      );
    }),
  );
};

function conToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
