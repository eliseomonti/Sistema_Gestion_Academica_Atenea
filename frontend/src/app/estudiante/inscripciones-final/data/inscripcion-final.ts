import { EstadoInscripcion } from "../../inscripciones-comision/data/inscripcion-comision";

export interface InscripcionFinalResponse{
    id: number;
    examenFinalId: number;
    materiaId: number;
    nombreMateria: string;
    fecha: string;
    horaInicio: string;
    horaFin: string;
    nombreAula: string | null;
    fechaInscripcion: string;
    estado: EstadoInscripcion;
}

export interface DocenteMesa{
    nombre: string;
    apellido: string;
}

export interface OfertaFinalResponse{
    planEstudioId: number;
    examenFinalId: number;
    materiaId: number;
    nombreMateria: string;
    fecha: string;
    horaInicio: string;
    horaFin: string;
    nombreAula: string | null;
    docentes: DocenteMesa[];
    inscripto: boolean;
}

export interface InscripcionFinalRequest
{
    examenFinalId: number;
}