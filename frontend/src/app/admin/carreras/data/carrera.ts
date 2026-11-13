import { PeriodoLectivo } from '../../../shared/models/periodo-lectivo';

export type ModalidadCarrera = 'PRESENCIAL' | 'VIRTUAL';

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
