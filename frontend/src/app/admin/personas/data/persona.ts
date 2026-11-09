export interface DatosPersonalesRequest {
  nombre: string;
  apellido: string;
  dni: string;
  telefono: string;
  fechaNacimiento: string;
  email: string;
}

export interface PersonaResponse {
  id: number;
  nombre: string;
  apellido: string;
  dni: string;
  telefono: string;
  fechaNacimiento: string;
  email: string;
  legajoEstudiante: number | null;
  legajoDocente: number | null;
  activo: boolean;
}

export interface AltaPersonaResponse {
  personaId: number;
  legajo: number;
  username: string;
  passwordTemporal: string;
}