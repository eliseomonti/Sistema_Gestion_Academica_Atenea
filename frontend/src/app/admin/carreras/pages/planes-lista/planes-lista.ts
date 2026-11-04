import { Component, computed, effect, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { PlanEstudioResponse } from '../../data/carrera';
import { CarrerasApi } from '../../data/carreras-api';

/**
 * Los planes vigentes de una carrera. Si tiene uno solo, no muestra la lista: va directo a las
 * materias de ese plan.
 */
@Component({
  selector: 'app-planes-lista',
  imports: [RouterLink, Tabla, Icono],
  templateUrl: './planes-lista.html',
  styleUrl: './planes-lista.css',
})
export class PlanesLista {
  private readonly api = inject(CarrerasApi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  // La URL trae el id como texto: numberAttribute lo pasa a número.
  readonly carreraId = input.required({ transform: numberAttribute });

  /** Se vuelve a pedir si cambia el id de la URL. */
  protected readonly planes = rxResource({
    params: () => this.carreraId(),
    stream: ({ params: carreraId }) => this.api.listarPlanes(carreraId),
  });

  /**
   * El backend no tiene un endpoint para una sola carrera: el nombre se busca en la lista, que es
   * chica. Si todavía no llegó, el título se muestra sin el nombre.
   */
  private readonly carreras = rxResource({ stream: () => this.api.listarCarreras() });
  protected readonly nombreCarrera = computed(
    () => this.carreras.value()?.find((carrera) => carrera.id === this.carreraId())?.nombre,
  );

  /** El backend devuelve todos los planes; se muestran sólo los vigentes. */
  protected readonly vigentes = computed(() =>
    (this.planes.value() ?? []).filter((plan) => plan.vigente),
  );

  /** Con un solo plan vigente se redirige: mientras tanto no se muestra la tabla. */
  protected readonly redirigiendo = computed(() => this.vigentes().length === 1);

  protected readonly columnas: Columna<PlanEstudioResponse>[] = [
    { clave: 'nombre', titulo: 'Plan' },
  ];

  /** Cada fila lleva a las materias del plan: /admin/carreras/:carreraId/planes/:planId/materias. */
  protected readonly enlace = (plan: PlanEstudioResponse) => [plan.id, 'materias'];

  constructor() {
    effect(() => {
      const vigentes = this.vigentes();
      if (vigentes.length === 1) {
        // replaceUrl: esta pantalla no queda en el historial, así "atrás" vuelve a Carreras.
        void this.router.navigate([vigentes[0].id, 'materias'], {
          relativeTo: this.ruta,
          replaceUrl: true,
        });
      }
    });
  }
}
