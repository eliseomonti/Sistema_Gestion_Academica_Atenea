import { PeriodoLectivo } from "../../inscripciones-comision/data/inscripcion-comision";

export type CondicionExamen = 'PRESENTE' | 'AUSENTE';
export type CondicionCursada = 'REGULAR' | 'PROMOCIONADO' | 'NO_REGULAR';
export type TipoEvaluacion = 'PARCIAL' | 'TRABAJO_PRACTICO' | 'TRABAJO_FINAL' | 'EXPOSICION_ORAL' | 'COLOQUIO';


export interface NotaEvaluacionEstudianteResponse{
    evaluacionId: number;
    nombre: string;
    tipo: TipoEvaluacion;
    fecha: string;
    evaluacionRecuperadaId: number | null;
    condicion: CondicionExamen | null;
    nota: number | null;
}

export interface CierreCursadaEstudianteResponse{
    condicion: CondicionCursada;
    nota: number | null;
    fechaCierre: string;
    fechaVencimiento: string | null;
}

export interface CursadaEstudianteResponse{
    inscripcionComisionId: number;
    materiaId: number;
    nombreMateria: string;
    nombreComision: string;
    anioLectivo: number;
    periodoLectivo: PeriodoLectivo;
    evaluaciones: NotaEvaluacionEstudianteResponse[];
    cierre: CierreCursadaEstudianteResponse | null;
}

export interface FinalEstudianteResponse{
    inscripcionExamenFinalId: number;
    examenFinalId: number;
    materiaId: number;
    nombreMateria: string;
    fecha: string;
    condicion: CondicionExamen | null;
    nota: number | null;
    aprobado: boolean | null;
}