import { NotaEstudianteResponse } from '../../evaluaciones/data/evaluacion';

/** Una mesa en cuyo tribunal está el docente. nombreAula en null: la mesa es virtual. */
export interface MesaDocenteResponse {
  examenFinalId: number;
  materiaId: number;
  nombreMateria: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  nombreAula: string | null;
}

/**
 * La planilla de una mesa: los anotados que no se dieron de baja, por apellido. Cada fila es igual a
 * la de la planilla de una evaluación.
 */
export interface NotasExamenFinalResponse {
  examenFinalId: number;
  materiaId: number;
  nombreMateria: string;
  fecha: string;
  notas: NotaEstudianteResponse[];
}
