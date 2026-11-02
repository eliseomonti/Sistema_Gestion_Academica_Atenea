import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../http/api-url';
import { LoginRequest, RefreshRequest, TokenResponse } from './auth';

/** Endpoints de /api/auth. Las pantallas no lo usan directo: pasan por `Sesion`. */
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
}
