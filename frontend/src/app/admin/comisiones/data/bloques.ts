import { Turno } from '../../../shared/models/turno';

/** Una franja horaria de la planilla, por ejemplo de 08:30 a 10:30. */
export interface Bloque {
  inicio: string;
  fin: string;
}

/**
 * Los bloques de cada turno, como en la planilla de cursadas. No vienen del backend, que acepta
 * cualquier horario: si la facultad los cambia, se cambian acá.
 */
export const BLOQUES: Record<Turno, Bloque[]> = {
  MANANA: [
    { inicio: '08:30', fin: '10:30' },
    { inicio: '11:00', fin: '13:00' },
  ],
  MEDIA_MANANA: [
    { inicio: '10:30', fin: '12:30' },
    { inicio: '13:00', fin: '15:00' },
  ],
  TARDE: [
    { inicio: '13:00', fin: '15:00' },
    { inicio: '15:30', fin: '17:30' },
  ],
  NOCHE: [
    { inicio: '18:00', fin: '20:00' },
    { inicio: '20:15', fin: '22:15' },
  ],
};
