import { DiaSemana } from "../../../shared/models/dia-semana";
import { RolDocente } from "../../../shared/models/rol-docente";
import { Turno } from "../../../shared/models/turno";

export type PeriodoLectivo = 'PRIMER_CUATRIMESTRE' | 'SEGUNDO_CUATRIMESTRE' | 'ANUAL';

export interface OfertaMateriaResponse{
    planEstudioId: number;
    materiaId: number;
    nombreMateria: string;
    anioCursado: number;
    periodoLectivo: PeriodoLectivo;
    comisiones: OfertaComisionResponse[];
}


export interface OfertaComisionResponse{
    comisionMateriaId: number;
    nombreComision: string;
    turno: Turno;
    horarios: OfertaHorarioResponse[]; 
    docentes: OfertaDocenteResponse[];
    cupoMaximo: number | null;
    cupoDisponible: number | null;
    inscripto: boolean;
}

export interface OfertaHorarioResponse{
    diaSemana: DiaSemana;
    horaInicio: string;
    horaFin: string;
    aula: string | null;
}

export interface OfertaDocenteResponse{
    apellido: string;
    nombre: string;
    rol: RolDocente;
}

export interface InscripcionComisionRequest{
    comisionMateriaId: number;
}

export interface InscripcionComisionResponse{
    id: number;
    comisionMateriaId: number;
    nombreComision: string;
    materiaId: number;
    nombreMateria: string;
    periodoLectivo: PeriodoLectivo;
    anioLectivo: number;
    fechaInscripcion: string;
    estado: string;
}