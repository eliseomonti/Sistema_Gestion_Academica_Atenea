import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Pestana, Pestanas } from '../../../../shared/ui/pestanas/pestanas';

/** El título y una pestaña por sección. La pestaña elegida se muestra en el <router-outlet>. */
@Component({
  selector: 'app-inscripcion-finales',
  imports: [RouterOutlet, Pestanas],
  templateUrl: './inscripcion-finales.html',
})
export class InscripcionFinales {
  protected readonly pestanas: Pestana[] = [
    { texto: 'Mesas disponibles', ruta: 'mesas' },
    { texto: 'Mis inscripciones', ruta: 'mis-inscripciones' },
  ];
}
