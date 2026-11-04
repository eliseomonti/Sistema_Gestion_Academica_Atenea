export type ModalidadCarrera = 'PRESENCIAL' | 'VIRTUAL';

/** Mitad del año lectivo en que se dicta una materia, no el cuatrimestre de la carrera. */
export type PeriodoLectivo = 'PRIMER_CUATRIMESTRE' | 'SEGUNDO_CUATRIMESTRE' | 'ANUAL';

/** Igual que `CarreraResponse` del backend. */
export interface CarreraResponse {
  id: number;
  nombre: string;
  modalidad: ModalidadCarrera;
  activa: boolean;
}

/** Igual que `PlanEstudioResponse` del backend. */
export interface PlanEstudioResponse {
  id: number;
  nombre: string;
  vigente: boolean;
}

/** Igual que `PlanMateriaResponse` del backend. */
export interface PlanMateriaResponse {
  materiaId: number;
  codigo: string;
  nombre: string;
  anioCursado: number;
  periodoLectivo: PeriodoLectivo;
}
