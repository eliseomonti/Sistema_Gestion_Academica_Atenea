import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { EvaluacionResponse } from '../../comisiones-materia/data/comision-materia';
import {
  CargaNotasRequest,
  ModificacionEvaluacionRequest,
  NotasEvaluacionResponse,
} from './evaluacion';

/** Una evaluación ya creada y sus notas. El listado y el alta están en ComisionesMateriaApi. */
@Service()
export class EvaluacionesApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/docente/evaluaciones`;

  modificar(
    evaluacionId: number,
    request: ModificacionEvaluacionRequest,
  ): Observable<EvaluacionResponse> {
    return this.http.put<EvaluacionResponse>(`${this.url}/${evaluacionId}`, request);
  }

  /** Borrado real. Si tiene notas o recuperatorios, el backend responde con un error. */
  eliminar(evaluacionId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${evaluacionId}`);
  }

  /** La planilla: todos los anotados a la materia, con su nota si ya está cargada. */
  notas(evaluacionId: number): Observable<NotasEvaluacionResponse> {
    return this.http.get<NotasEvaluacionResponse>(`${this.url}/${evaluacionId}/notas`);
  }

  /** Carga o corrige las notas que vienen. Devuelve la planilla completa, ya actualizada. */
  cargarNotas(evaluacionId: number, request: CargaNotasRequest): Observable<NotasEvaluacionResponse> {
    return this.http.put<NotasEvaluacionResponse>(`${this.url}/${evaluacionId}/notas`, request);
  }
}
