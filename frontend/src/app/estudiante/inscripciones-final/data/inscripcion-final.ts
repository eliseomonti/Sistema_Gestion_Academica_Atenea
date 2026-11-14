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