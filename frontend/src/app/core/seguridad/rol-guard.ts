import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { Rol, rutaLogin } from './rol';
import { Sesion } from './sesion';

/**
 * Deja entrar a las rutas de un actor sólo si el usuario tiene ese rol; si no, lo manda a su login.
 */
export function rolGuard(rol: Rol): CanMatchFn {
  return () => inject(Sesion).tieneRol(rol) || inject(Router).createUrlTree(rutaLogin(rol));
}
