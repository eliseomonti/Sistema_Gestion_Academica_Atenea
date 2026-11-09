import { Pipe, PipeTransform } from '@angular/core';
import { Turno } from '../models/turno';

const TEXTOS: Record<Turno, string> = {
  MANANA: "Mañana",
  MEDIA_MANANA: "Media mañana",
  TARDE: "Tarde",
  NOCHE: "Noche"
}

@Pipe({
  name: 'turno',
})
export class TurnoPipe implements PipeTransform {
  transform(turno: Turno): string {
    return TEXTOS[turno];
  }
}
