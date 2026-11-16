export interface DocenteTribunalResponse {
    personaId: number;
    apellido: string;
    nombre: string;
}

export interface ExamenFinalResponse {
    id: number;
    periodoId: number;
    materiaId: number;
    nombreMateria: string;
    fecha: string;
    horaInicio: string;
    horaFin: string;
    aulaId: number | null;
    nombreAula: string | null;
    docentes: DocenteTribunalResponse[];
}

export interface AltaExamenFinalRequest {
    periodoId: number;
    materiaId: number;
    fecha: string;
    horaInicio: string;
    horaFin: string;
    aulaId: number | null;
    docenteIds: number[];
}

export interface ModificacionExamenFinalRequest {
    fecha: string;
    horaInicio: string;
    horaFin: string;
    aulaId: number | null;
    docenteIds: number[];
}