import { Component, computed, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Pestana, Pestanas } from '../../../../shared/ui/pestanas/pestanas';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

/**
 * Una materia que dicta el docente: arriba la materia y la comisión, y debajo una pestaña por
 * sección. La pestaña elegida se muestra en el <router-outlet>.
 */
@Component({
  selector: 'app-comision-materia-detalle',
  imports: [RouterLink, RouterOutlet, Icono, Pestanas, TurnoPipe],
  templateUrl: './comision-materia-detalle.html',
  styleUrl: './comision-materia-detalle.css',
})
export class ComisionMateriaDetalle {
  private readonly api = inject(ComisionesMateriaApi);

  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  /** Cada pestaña es una ruta hija. */
  protected readonly pestanas: Pestana[] = [
    { texto: 'Estudiantes', ruta: 'estudiantes' },
    { texto: 'Evaluaciones', ruta: 'evaluaciones' },
    { texto: 'Cierre', ruta: 'cierre' },
  ];

  /**
   * Para el título y el subtítulo. El backend no tiene un endpoint para una sola materia: se busca
   * en la lista de Mis comisiones, que es chica.
   */
  private readonly materias = rxResource({ stream: () => this.api.listar() });

  protected readonly materia = computed(() =>
    this.materias
      .value()
      ?.find((materia) => materia.comisionMateriaId === this.comisionMateriaId()),
  );
}
