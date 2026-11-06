export interface DocenteListadoResponse {
  personaId: number;
  legajo: number;
  apellido: string;
  nombre: string;
  dni: string;
  email: string;
}

export interface DocentesFiltro {
  buscar?: string;
  activo?: boolean;
  page?: number;
  size?: number;
}