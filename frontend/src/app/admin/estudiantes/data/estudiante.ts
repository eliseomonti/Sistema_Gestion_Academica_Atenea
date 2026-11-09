import { DatosPersonalesRequest } from '../../personas/data/persona';

export type EstadoInscripcionCarrera = 'ACTIVA' | 'EGRESADO' | 'BAJA';

/** Igual que `AltaEstudianteRequest` del backend. La carrera no va: el backend la saca del plan. */
export interface AltaEstudianteRequest {
  datosPersonales: DatosPersonalesRequest;
  planEstudioId: number;
}

/** Igual que `InscripcionCarreraRequest` del backend. */
export interface InscripcionCarreraRequest {
  planEstudioId: number;
}

/** Igual que `VerificacionInscripcionResponse` del backend. `motivo` es null cuando se puede. */
export interface VerificacionInscripcionResponse {
  puedeInscribirse: boolean;
  motivo: string | null;
}

/** Igual que `InscripcionCarreraResponse` del backend. */
export interface InscripcionCarreraResponse {
  id: number;
  personaId: number;
  legajoEstudiante: number;
  planEstudioId: number;
  estado: EstadoInscripcionCarrera;
  /** Fecha ISO, por ejemplo '2026-11-08'. */
  fechaInscripcion: string;
}

/** Igual que `EstudianteListadoResponse` del backend. */
export interface EstudianteListadoResponse {
  personaId: number;
  legajo: number;
  apellido: string;
  nombre: string;
  dni: string;
  email: string;
}

/** Los parámetros del listado. Los que quedan sin poner, el backend no los usa para filtrar. */
export interface FiltrosEstudiantes {
  /** Palabras del apellido o el nombre, el comienzo del DNI o el legajo exacto. */
  buscar?: string;
  carreraId?: number;
  planEstudioId?: number;
  /** Si no se pone, el backend usa ACTIVA. */
  estado?: EstadoInscripcionCarrera;
  /** Empieza en 0. */
  pagina?: number;
}
