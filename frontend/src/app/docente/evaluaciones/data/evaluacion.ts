import { TipoEvaluacion } from '../../comisiones-materia/data/comision-materia';

/** El tipo y la evaluación que recupera no cambian: para eso se borra y se crea otra. */
export interface ModificacionEvaluacionRequest {
  nombre: string;
  fecha: string;
}

export type CondicionExamen = 'PRESENTE' | 'AUSENTE';

/**
 * Una fila de la planilla: el estudiante y su nota vigente. condicion y nota en null: todavía no
 * está cargada; nota en null con AUSENTE: no se presentó. inscripcionId es el que se manda para
 * cargarla.
 */
export interface NotaEstudianteResponse {
  inscripcionId: number;
  personaId: number;
  legajo: number;
  apellido: string;
  nombre: string;
  condicion: CondicionExamen | null;
  nota: number | null;
}

/** La planilla de una evaluación: todos los anotados a la materia, por apellido. */
export interface NotasEvaluacionResponse {
  evaluacionId: number;
  nombre: string;
  tipo: TipoEvaluacion;
  fecha: string;
  notas: NotaEstudianteResponse[];
}

/** PRESENTE lleva nota (de 0 a 10); AUSENTE no. */
export interface NotaRequest {
  inscripcionId: number;
  condicion: CondicionExamen;
  nota: number | null;
}

/** Se puede mandar una parte de la planilla: las inscripciones que no vienen quedan como estaban. */
export interface CargaNotasRequest {
  notas: NotaRequest[];
}
