import { Turno } from "../../../shared/models/turno";


export interface ComisionResponse{
    id: number;
    nombre: string;
    planEstudioId: number;
    anioCursado: number;
    nroComision: number;
    turno: Turno;
    anioLectivo: number;
    cantidadMaterias: number;
    activa: boolean;
}

export interface ComisionesFiltros{
    carreraId: number;
    planEstudioId?: number;
    anioLectivo?: number;
    anioCursado?: number;
    turno?: Turno;
    activa?: boolean;
}

export interface AltaComisionRequest{
    planEstudioId: number;
    anioCursado: number;
    nroComision: number;
    turno: Turno;
    anioLectivo: number;
}

export interface ModificacionComisionRequest {
  nroComision: number;
  turno: Turno;
}