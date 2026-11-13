import { DiaSemana } from '../../../shared/models/dia-semana';
import { PeriodoLectivo } from '../../../shared/models/periodo-lectivo';
import { RolDocente } from '../../../shared/models/rol-docente';
import { Turno } from '../../../shared/models/turno';

/** Una materia de comisión que dicta el docente logueado, con su rol en ella y los horarios. */
export interface MateriaDocenteResponse {
  comisionMateriaId: number;
  comisionId: number;
  nombreComision: string;
  turno: Turno;
  materiaId: number;
  nombreMateria: string;
  anioLectivo: number;
  periodoLectivo: PeriodoLectivo;
  rol: RolDocente;
  horarios: HorarioResponse[];
}

/** Las horas son LocalTime de Java ("08:30" o "08:30:00"). Sin aula, la cursada es virtual. */
export interface HorarioResponse {
  id: number;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  aulaId: number | null;
  aulaNombre: string | null;
}

/** Un estudiante anotado a la materia de la comisión. */
export interface EstudianteCursadaResponse {
  inscripcionComisionId: number;
  personaId: number;
  legajo: number;
  apellido: string;
  nombre: string;
  email: string;
}

export type TipoEvaluacion =
  | 'PARCIAL'
  | 'TRABAJO_PRACTICO'
  | 'TRABAJO_FINAL'
  | 'EXPOSICION_ORAL'
  | 'COLOQUIO';

export const TIPOS_EVALUACION: Record<TipoEvaluacion, string> = {
  PARCIAL: 'Parcial',
  TRABAJO_PRACTICO: 'Trabajo práctico',
  TRABAJO_FINAL: 'Trabajo final',
  EXPOSICION_ORAL: 'Exposición oral',
  COLOQUIO: 'Coloquio',
};

/**
 * Una evaluación de la materia. La fecha es un LocalDate de Java ("2026-05-14").
 * evaluacionRecuperadaId, sólo si es un recuperatorio: entonces el tipo es el de la que recupera.
 */
export interface EvaluacionResponse {
  id: number;
  comisionMateriaId: number;
  nombre: string;
  tipo: TipoEvaluacion;
  fecha: string;
  evaluacionRecuperadaId: number | null;
}

/** Con evaluacionRecuperadaId es un recuperatorio: el tipo va en null y el backend copia el de esa. */
export interface AltaEvaluacionRequest {
  nombre: string;
  fecha: string;
  tipo: TipoEvaluacion | null;
  evaluacionRecuperadaId: number | null;
}

export type CondicionCursada = 'REGULAR' | 'PROMOCIONADO' | 'NO_REGULAR';

export const CONDICIONES_CURSADA: Record<CondicionCursada, string> = {
  REGULAR: 'Regular',
  PROMOCIONADO: 'Promocionado',
  NO_REGULAR: 'No regular',
};

/**
 * Una fila del acta: el estudiante y cómo terminó su cursada. condicion y fechas en null: todavía
 * no se cerró. fechaVencimiento, sólo si es REGULAR. Las fechas son LocalDate ("2026-11-13").
 */
export interface CierreEstudianteResponse {
  inscripcionComisionId: number;
  personaId: number;
  legajo: number;
  apellido: string;
  nombre: string;
  condicion: CondicionCursada | null;
  nota: number | null;
  fechaCierre: string | null;
  fechaVencimiento: string | null;
}

/** El acta de cierre de la materia: todos los anotados que no se dieron de baja, por apellido. */
export interface ActaCursadaResponse {
  comisionMateriaId: number;
  estudiantes: CierreEstudianteResponse[];
}

/** La nota es la de cursada: obligatoria en PROMOCIONADO, opcional en las otras. */
export interface CierreRequest {
  inscripcionComisionId: number;
  condicion: CondicionCursada;
  nota: number | null;
}

/** Se puede mandar una parte del acta: las inscripciones que no vienen quedan como estaban. */
export interface CargaCierreRequest {
  cierres: CierreRequest[];
}
