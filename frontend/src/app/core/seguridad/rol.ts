/** Los roles del backend (`NombreRol`), sin el prefijo `ROLE_` que usa Spring. */
export type Rol = 'ADMIN' | 'DOCENTE' | 'ESTUDIANTE';

/** Pantalla de login de cada rol: /login/admin, /login/docente o /login/estudiante. */
export function rutaLogin(rol: Rol): string[] {
  return ['/login', rol.toLowerCase()];
}

/** Pantalla a la que se entra después del login: /admin, /docente o /estudiante. */
export function rutaInicio(rol: Rol): string[] {
  return ['/', rol.toLowerCase()];
}
