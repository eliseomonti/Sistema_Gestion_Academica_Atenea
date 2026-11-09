import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { Pagina } from '../../../shared/models/pagina';
import { AltaPersonaResponse } from '../../personas/data/persona';
import {
  AltaEstudianteRequest,
  EstudianteListadoResponse,
  FiltrosEstudiantes,
  InscripcionCarreraDetalleResponse,
  InscripcionCarreraRequest,
  InscripcionCarreraResponse,
  VerificacionInscripcionResponse,
} from './estudiante';

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
    if (filtros.planEstudioId !== undefined) {
      params = params.set('planEstudioId', filtros.planEstudioId);
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.pagina !== undefined) {
      params = params.set('page', filtros.pagina);
    }
    return this.http.get<Pagina<EstudianteListadoResponse>>(this.url, { params });
  }

  /** Una persona nueva, que queda ya inscripta al plan. Devuelve su usuario y contraseña temporal. */
  alta(request: AltaEstudianteRequest): Observable<AltaPersonaResponse> {
    return this.http.post<AltaPersonaResponse>(this.url, request);
  }

  /** Si la persona se puede inscribir en el plan, sin inscribirla. Si no se puede, trae el motivo. */
  verificarInscripcion(
    personaId: number,
    planEstudioId: number,
  ): Observable<VerificacionInscripcionResponse> {
    return this.http.get<VerificacionInscripcionResponse>(
      `${this.url}/${personaId}/inscripciones/verificacion`,
      { params: { planEstudioId } },
    );
  }

  /**
   * Inscribe al plan a una persona que ya existe: un estudiante que suma una carrera, o alguien que
   * todavía no es estudiante (por ejemplo, un docente).
   */
  inscribir(
    personaId: number,
    request: InscripcionCarreraRequest,
  ): Observable<InscripcionCarreraResponse> {
    return this.http.post<InscripcionCarreraResponse>(
      `${this.url}/${personaId}/inscripciones`,
      request,
    );
  }

  listarInscripciones(personaId: number): Observable<InscripcionCarreraDetalleResponse[]>
  {
    return this.http.get<InscripcionCarreraDetalleResponse[]>(`${this.url}/${personaId}/inscripciones`)
  }

  darDeBajaInscripcion(personaId: number, inscripcionId: number): Observable<InscripcionCarreraResponse>
  {
    return this.http.post<InscripcionCarreraResponse>(`${this.url}/${personaId}/inscripciones/${inscripcionId}/baja`,null);
  }




}
