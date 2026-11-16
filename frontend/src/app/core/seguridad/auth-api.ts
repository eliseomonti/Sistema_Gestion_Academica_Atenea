import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../http/api-url';
import {
  CambioPasswordRequest,
  LinkPasswordRequest,
  LoginRequest,
  RecuperacionPasswordRequest,
  RefreshRequest,
  RestablecimientoPasswordRequest,
  TokenResponse,
} from './auth';

/** Endpoints de /api/auth y /api/cuenta. Las pantallas no lo usan directo: pasan por `Sesion`. */
@Service()
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/auth`;

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.url}/login`, request);
  }

  refresh(request: RefreshRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.url}/refresh`, request);
  }

  logout(request: RefreshRequest): Observable<void> {
    return this.http.post<void>(`${this.url}/logout`, request);
  }

  /** Responde igual exista o no el usuario: si existe, le llega un mail con el link. */
  recuperarPassword(request: RecuperacionPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.url}/recuperar-password`, request);
  }

  restablecerPassword(request: RestablecimientoPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.url}/restablecer-password`, request);
  }

  /** Lleva el token: no es de /auth, así que pasa por el interceptor. */
  cambiarPassword(request: CambioPasswordRequest): Observable<TokenResponse> {
    return this.http.put<TokenResponse>(`${API_URL}/cuenta/password`, request);
  }

  /** El mismo link que desde el login, pedido por el usuario logueado. Lleva el token. */
  pedirLinkPassword(request: LinkPasswordRequest): Observable<void> {
    return this.http.post<void>(`${API_URL}/cuenta/password/link`, request);
  }
}
