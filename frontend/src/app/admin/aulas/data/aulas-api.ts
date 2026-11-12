import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { AulaResponse } from './aula';

/** Endpoints de /api/admin/aulas. Las aulas se cargan por SQL: acá sólo se listan. */
@Service()
export class AulasApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/aulas`;

  /** Sólo las activas, ordenadas por nombre. */
  listar(): Observable<AulaResponse[]> {
    return this.http.get<AulaResponse[]>(this.url);
  }
}
