import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { Pagina } from '../../../shared/models/pagina';
import { EstudianteListadoResponse, FiltrosEstudiantes } from './estudiante';

/** Endpoints de /api/admin/estudiantes. */
@Service()
export class EstudiantesApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/admin/estudiantes`;

  /** Ordenados por apellido y nombre, de a 20 por página. */
  listar(filtros: FiltrosEstudiantes): Observable<Pagina<EstudianteListadoResponse>> {
    // HttpParams no agrega a la URL los parámetros que quedan sin poner.
    let params = new HttpParams();
    if (filtros.buscar?.trim()) {
      params = params.set('buscar', filtros.buscar.trim());
    }
    if (filtros.carreraId !== undefined) {
      params = params.set('carreraId', filtros.carreraId);
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.pagina !== undefined) {
      params = params.set('page', filtros.pagina);
    }
    return this.http.get<Pagina<EstudianteListadoResponse>>(this.url, { params });
  }
}
