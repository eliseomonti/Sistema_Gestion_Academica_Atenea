import { computed, inject, Service, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, throwError } from 'rxjs';
import { LoginRequest, TokenResponse } from './auth';
import { AuthApi } from './auth-api';
import { Rol } from './rol';

const CLAVE = 'atenea.sesion';

interface DatosSesion {
  accessToken: string;
  refreshToken: string;
  /** Con qué rol entró, para volver a su pantalla de login cuando la sesión vence. */
  rol: Rol;
}

/**
 * Los tokens del usuario logueado. Se guardan en localStorage para que la sesión siga abierta al
 * recargar la página o abrir otra pestaña.
 */
@Service()
export class Sesion {
  private readonly authApi = inject(AuthApi);
  private readonly datos = signal<DatosSesion | null>(leerGuardada());
  private refresco: Observable<string> | null = null;

  readonly accessToken = computed(() => this.datos()?.accessToken ?? null);
  readonly rol = computed(() => this.datos()?.rol ?? null);
  private readonly roles = computed(() => {
    const token = this.accessToken();
    return token ? leerRoles(token) : [];
  });

  constructor() {
    // Si otra pestaña inicia, renueva o cierra la sesión, esta toma lo que quedó guardado. Si no,
    // seguiría con un refresh token que el backend ya invalidó. El navegador sólo dispara este
    // evento en las otras pestañas, no en la que hizo el cambio.
    window.addEventListener('storage', (evento) => {
      if (evento.key === CLAVE || evento.key === null) {
        this.datos.set(leerGuardada());
      }
    });
  }

  tieneRol(rol: Rol): boolean {
    return this.roles().includes(rol);
  }

  /**
   * Hace el login. Emite `false` si las credenciales son correctas pero el usuario no tiene el rol
   * de esa pantalla de login (por ejemplo, un estudiante que entra por la de docentes).
   */
  iniciar(rol: Rol, credenciales: LoginRequest): Observable<boolean> {
    return this.authApi.login(credenciales).pipe(
      map((tokens) => {
        if (!leerRoles(tokens.accessToken).includes(rol)) {
          this.revocar(tokens.refreshToken);
          return false;
        }
        this.guardar(tokens, rol);
        return true;
      }),
    );
  }

  /**
   * Pide un access token nuevo con el refresh token. Si varios pedidos reciben un 401 a la vez,
   * comparten un solo refresh: el backend invalida el refresh token al usarlo, así que un segundo
   * refresh con el mismo token fallaría.
   */
  refrescar(): Observable<string> {
    const actual = this.datos();
    if (!actual) {
      return throwError(() => new Error('No hay una sesión iniciada'));
    }
    this.refresco ??= this.authApi.refresh({ refreshToken: actual.refreshToken }).pipe(
      map((tokens) => {
        this.guardar(tokens, actual.rol);
        return tokens.accessToken;
      }),
      catchError((error: unknown) => {
        // Otra pestaña pudo renovar al mismo tiempo con el mismo refresh token y ganarle. Si dejó
        // tokens nuevos guardados, se usan esos en vez de cerrar la sesión.
        const guardada = leerGuardada();
        if (guardada && guardada.refreshToken !== actual.refreshToken) {
          this.datos.set(guardada);
          return of(guardada.accessToken);
        }
        this.borrar();
        return throwError(() => error);
      }),
      finalize(() => (this.refresco = null)),
      shareReplay(1),
    );
    return this.refresco;
  }

  cerrar(): void {
    const actual = this.datos();
    if (actual) {
      this.revocar(actual.refreshToken);
    }
    this.borrar();
  }

  private guardar(tokens: TokenResponse, rol: Rol): void {
    const datos: DatosSesion = {
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      rol,
    };
    this.datos.set(datos);
    localStorage.setItem(CLAVE, JSON.stringify(datos));
  }

  private borrar(): void {
    this.datos.set(null);
    localStorage.removeItem(CLAVE);
  }

  /** Avisa al backend que el refresh token ya no se usa. Si falla, el token vence solo. */
  private revocar(refreshToken: string): void {
    this.authApi.logout({ refreshToken }).subscribe({ error: () => undefined });
  }
}

function leerGuardada(): DatosSesion | null {
  try {
    return JSON.parse(localStorage.getItem(CLAVE) ?? 'null') as DatosSesion | null;
  } catch {
    return null;
  }
}

/**
 * Lee los roles del access token (claim `roles`). No verifica la firma: eso lo hace el backend en
 * cada pedido. Acá sólo se usa para decidir qué pantallas mostrar.
 */
function leerRoles(accessToken: string): Rol[] {
  try {
    const payload = accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(payload)) as { roles?: string[] };
    return (claims.roles ?? []).map((rol) => rol.replace(/^ROLE_/, '') as Rol);
  } catch {
    return [];
  }
}
