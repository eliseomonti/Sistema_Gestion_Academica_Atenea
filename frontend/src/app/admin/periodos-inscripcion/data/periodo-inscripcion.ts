import { PeriodoLectivo } from "../../carreras/data/carrera";

export type TipoPeriodo = 'CURSADA' | 'FINAL';

export interface PeriodoInscripcionResponse{
    id: number;
    carreraId: number;
    tipo: TipoPeriodo;
    anioLectivo: number;
    periodoLectivo: PeriodoLectivo | null;
    fechaInicio: string;
    fechaCierre: string;
    habilitada: boolean;
}

export interface PeriodosFiltro{
    carreraId: number;
    anioLectivo?: number;
    tipo?: TipoPeriodo;
}

export interface AltaPeriodoRequest{
    carreraId: number;
    tipo: TipoPeriodo;
    anioLectivo: number;
    periodoLectivo: PeriodoLectivo | null;
    fechaInicio: string;
    fechaCierre: string;
}

export interface ModificacionPeriodoRequest{
    fechaInicio: string;
    fechaCierre: string;
}