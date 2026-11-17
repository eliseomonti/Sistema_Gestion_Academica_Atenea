import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Pestana, Pestanas } from '../../../../shared/ui/pestanas/pestanas';

/** El título y una pestaña por sección. La pestaña elegida se muestra en el <router-outlet>. */
@Component({
  selector: 'app-inscripcion-cursado',
  imports: [RouterOutlet, Pestanas],
  templateUrl: './inscripcion-cursado.html',
})
export class InscripcionCursado {
  protected readonly pestanas: Pestana[] = [
    { texto: 'Materias para inscribirte', ruta: 'oferta' },
    { texto: 'Mis inscripciones', ruta: 'mis-inscripciones' },
  ];
}
