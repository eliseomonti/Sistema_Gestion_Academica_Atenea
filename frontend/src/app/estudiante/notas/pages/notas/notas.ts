import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Pestana, Pestanas } from '../../../../shared/ui/pestanas/pestanas';

/** El título y una pestaña por sección. La pestaña elegida se muestra en el <router-outlet>. */
@Component({
  selector: 'app-notas',
  imports: [RouterOutlet, Pestanas],
  templateUrl: './notas.html',
})
export class Notas {
  protected readonly pestanas: Pestana[] = [
    { texto: 'Cursadas', ruta: 'cursadas' },
    { texto: 'Finales', ruta: 'finales' },
  ];
}
