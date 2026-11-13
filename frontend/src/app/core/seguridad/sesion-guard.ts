import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { Sesion } from './sesion';

/** Deja entrar con cualquier rol, mientras haya una sesión abierta. Si no, al login. */
export const sesionGuard: CanMatchFn = () =>
  inject(Sesion).accessToken() !== null || inject(Router).createUrlTree(['/login']);
