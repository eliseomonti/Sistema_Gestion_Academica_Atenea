export type EstadoInscripcionCarrera = 'ACTIVA' | 'EGRESADO' | 'BAJA';

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
  /** Si no se pone, el backend usa ACTIVA. */
  estado?: EstadoInscripcionCarrera;
  /** Empieza en 0. */
  pagina?: number;
}
