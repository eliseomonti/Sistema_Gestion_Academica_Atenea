import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { CarreraResponse, PlanEstudioResponse, PlanMateriaResponse } from './carrera';

/** Endpoints de /api/admin/carreras: carreras, sus planes y las materias de cada plan. */
@Service()
export class CarrerasApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/carreras`;

  listarCarreras(): Observable<CarreraResponse[]> {
    return this.http.get<CarreraResponse[]>(this.url);
  }

  /** Todos los planes de la carrera, vigentes o no, del más nuevo al más viejo. */
  listarPlanes(carreraId: number): Observable<PlanEstudioResponse[]> {
    return this.http.get<PlanEstudioResponse[]>(`${this.url}/${carreraId}/planes`);
  }

  listarMaterias(carreraId: number, planId: number): Observable<PlanMateriaResponse[]> {
    return this.http.get<PlanMateriaResponse[]>(
      `${this.url}/${carreraId}/planes/${planId}/materias`,
    );
  }
}
