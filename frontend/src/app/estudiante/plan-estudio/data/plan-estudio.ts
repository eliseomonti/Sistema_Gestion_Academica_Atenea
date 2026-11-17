import { PeriodoLectivo } from "../../inscripciones-comision/data/inscripcion-comision";

export type EstadoMateria = 'APROBADA' | 'REGULAR' | 'CURSANDO' | 'REGULARIDAD_VENCIDA' | 'NO_REGULAR' | 'NO_CURSADA';

export interface PlanEstudianteResponse{
    planEstudioId: number;
    nombreCarrera: string;
    nombrePlan: string;
    materiasAprobadas: number;
    totalMaterias: number;
    porcentajeAvance: number;
    materias: MateriaPlanEstudianteResponse[];
}

export interface MateriaPlanEstudianteResponse{
    materiaId: number;
    nombre: string;
    anioCursado: number;
    periodoLectivo: PeriodoLectivo;
    estado: EstadoMateria;
    nota: number | null;
    vencimientoRegularidad: string | null;
}

/** Una fila del régimen de correlatividades: los códigos de las materias que exige para cursar y para rendir. */
export interface CorrelatividadesMateriaEstudianteResponse {
    materiaId: number;
    codigo: string | null;
    nombre: string;
    anioCursado: number;
    periodoLectivo: PeriodoLectivo;
    cursadasParaCursar: string[];
    aprobadasParaCursar: string[];
    cursadasParaRendir: string[];
    aprobadasParaRendir: string[];
}
