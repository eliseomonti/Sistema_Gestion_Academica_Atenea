/** Igual que `AulaResponse` del backend: una opción del desplegable de aulas de un horario. */
export interface AulaResponse {
  id: number;
  nombre: string;
  // null si no está relevada.
  capacidad: number | null;
}
