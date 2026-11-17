import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Pestana, Pestanas } from '../../../../shared/ui/pestanas/pestanas';

/** El título y una pestaña por sección. La pestaña elegida se muestra en el <router-outlet>. */
@Component({
  selector: 'app-plan-estudio',
  imports: [RouterOutlet, Pestanas],
  templateUrl: './plan-estudio.html',
})
export class PlanEstudio {
  protected readonly pestanas: Pestana[] = [
    { texto: 'Materias', ruta: 'materias' },
    { texto: 'Correlatividades', ruta: 'correlatividades' },
  ];
}
