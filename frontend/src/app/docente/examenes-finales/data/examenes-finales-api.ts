import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { CargaNotasRequest } from '../../evaluaciones/data/evaluacion';
import { MesaDocenteResponse, NotasExamenFinalResponse } from './examen-final';

@Service()
export class ExamenesFinalesApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/docente/examenes-finales`;

  /** Sin año, el backend usa el actual. Vienen por fecha y hora. El docente sale del token. */
  listar(anio?: number): Observable<MesaDocenteResponse[]> {
    let params = new HttpParams();
    if (anio !== undefined) {
      params = params.set('anio', anio);
    }
    return this.http.get<MesaDocenteResponse[]>(this.url, { params });
  }

  /** La planilla: los anotados a la mesa, con su nota si ya está cargada. */
  notas(examenFinalId: number): Observable<NotasExamenFinalResponse> {
    return this.http.get<NotasExamenFinalResponse>(`${this.url}/${examenFinalId}/notas`);
  }

  /** Carga o corrige las notas que vienen. Devuelve la planilla completa, ya actualizada. */
  cargarNotas(
    examenFinalId: number,
    request: CargaNotasRequest,
  ): Observable<NotasExamenFinalResponse> {
    return this.http.put<NotasExamenFinalResponse>(`${this.url}/${examenFinalId}/notas`, request);
  }
}
