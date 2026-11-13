import { Turno } from "../../../shared/models/turno";
import { PeriodoLectivo } from "../../carreras/data/carrera";


export interface ComisionResponse{
    id: number;
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

/** Sin año lectivo: el backend usa siempre el actual. */
export interface AltaComisionRequest{
    planEstudioId: number;
    anioCursado: number;
    nroComision: number;
    turno: Turno;
}

export interface ModificacionComisionRequest {
  nroComision: number;
  turno: Turno;
}

export interface ComisionDetalleResponse{
    id: number;
    planEstudioId: number;
    anioCursado: number;
    nroComision: number;
    turno: Turno;
    anioLectivo: number;
    activa: boolean;
    fechaBaja: string | null;
    materias: ComisionMateriaResponse[];
}

export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES' | 'SABADO' | 'DOMINGO';

export type RolDocente = 'TITULAR' | 'AYUDANTE';

/** Una materia de la comisión, con sus docentes y horarios: con esto se arma la grilla. */
export interface ComisionMateriaResponse {
    id: number;
    materiaId: number;
    nombreMateria: string;
    periodoLectivo: PeriodoLectivo;
    // null = sin límite de cupo.
    cupoMaximo: number | null;
    docentes: DocenteAsignadoResponse[];
    horarios: HorarioResponse[];
}

export interface DocenteAsignadoResponse {
    docenteId: number;
    apellido: string;
    nombre: string;
    rol: RolDocente;
}

/** Las horas son LocalTime de Java ("08:30" o "08:30:00"). Sin aula, la cursada es virtual. */
export interface HorarioResponse {
    id: number;
    diaSemana: DiaSemana;
    horaInicio: string;
    horaFin: string;
    aulaId: number | null;
    aulaNombre: string | null;
}

/** Docentes y horarios pueden ir vacíos y completarse después con la modificación. */
export interface AltaComisionMateriaRequest {
    materiaId: number;
    cupoMaximo: number | null;
    docentes: DocenteAsignadoRequest[];
    horarios: HorarioRequest[];
}

/** Reemplaza cupo, docentes y horarios: lo que no se manda, se quita. La materia no cambia. */
export interface ModificacionComisionMateriaRequest {
    cupoMaximo: number | null;
    docentes: DocenteAsignadoRequest[];
    horarios: HorarioRequest[];
}

export interface DocenteAsignadoRequest {
    docenteId: number;
    rol: RolDocente;
}

/** Las horas como las da un <input type="time"> ("08:30"). aulaId null = cursada virtual. */
export interface HorarioRequest {
    diaSemana: DiaSemana;
    horaInicio: string;
    horaFin: string;
    aulaId: number | null;
}