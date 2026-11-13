import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import {
  ActaCursadaResponse,
  AltaEvaluacionRequest,
  CargaCierreRequest,
  EstudianteCursadaResponse,
  EvaluacionResponse,
  MateriaDocenteResponse,
} from './comision-materia';

@Service()
export class ComisionesMateriaApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_URL}/docente/comisiones-materia`;

  /** Sin año lectivo, el backend usa el actual. El docente sale del token. */
  listar(anioLectivo?: number): Observable<MateriaDocenteResponse[]> {
    let params = new HttpParams();
    if (anioLectivo !== undefined) {
      params = params.set('anioLectivo', anioLectivo);
    }
    return this.http.get<MateriaDocenteResponse[]>(this.url, { params });
  }

  listarEstudiantes(comisionMateriaId: number): Observable<EstudianteCursadaResponse[]> {
    return this.http.get<EstudianteCursadaResponse[]>(`${this.url}/${comisionMateriaId}/estudiantes`);
  }

  /** Por fecha: el orden en que se toman. */
  listarEvaluaciones(comisionMateriaId: number): Observable<EvaluacionResponse[]> {
    return this.http.get<EvaluacionResponse[]>(`${this.url}/${comisionMateriaId}/evaluaciones`);
  }

  altaEvaluacion(
    comisionMateriaId: number,
    request: AltaEvaluacionRequest,
  ): Observable<EvaluacionResponse> {
    return this.http.post<EvaluacionResponse>(`${this.url}/${comisionMateriaId}/evaluaciones`, request);
  }

  /** El acta de cierre: todos los anotados, con su condición si ya se cerró su cursada. */
  acta(comisionMateriaId: number): Observable<ActaCursadaResponse> {
    return this.http.get<ActaCursadaResponse>(`${this.url}/${comisionMateriaId}/cierre`);
  }

  /** Cierra o corrige las condiciones que vienen. Devuelve el acta completa, ya actualizada. */
  cerrar(comisionMateriaId: number, request: CargaCierreRequest): Observable<ActaCursadaResponse> {
    return this.http.put<ActaCursadaResponse>(`${this.url}/${comisionMateriaId}/cierre`, request);
  }
}
