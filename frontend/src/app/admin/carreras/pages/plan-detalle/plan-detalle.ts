import { Component, computed, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Icono } from '../../../../shared/ui/icono/icono';
import { CarrerasApi } from '../../data/carreras-api';

/**
 * Un plan de estudio: arriba la carrera y el plan, y debajo una pestaña por sección. La pestaña
 * elegida se muestra en el <router-outlet>. Sus rutas están en admin.routes.ts, porque las
 * pestañas pueden ser pantallas de otras features (Estudiantes, Comisiones...).
 */
@Component({
  selector: 'app-plan-detalle',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, Icono],
  templateUrl: './plan-detalle.html',
  styleUrl: './plan-detalle.css',
})
export class PlanDetalle {
  private readonly api = inject(CarrerasApi);

  readonly carreraId = input.required({ transform: numberAttribute });
  readonly planId = input.required({ transform: numberAttribute });

  /** Cada pestaña es una ruta hija: 'materias' → /admin/carreras/:carreraId/planes/:planId/materias. */
  protected readonly pestanas = [
    { texto: 'Materias', ruta: 'materias' },
    { texto: 'Estudiantes', ruta: 'estudiantes' },
    { texto: 'Comisiones', ruta: 'comisiones'},
    { texto: 'Periodos', ruta: 'periodos'},
  ];

  /**
   * Para el título y el subtítulo, y para saber a dónde vuelve el link. El backend no tiene
   * endpoints para una sola carrera o un solo plan: se buscan en las listas, que son chicas.
   */
  private readonly carreras = rxResource({ stream: () => this.api.listarCarreras() });
  private readonly planes = rxResource({
    params: () => this.carreraId(),
    stream: ({ params: carreraId }) => this.api.listarPlanes(carreraId),
  });

  protected readonly nombreCarrera = computed(
    () => this.carreras.value()?.find((carrera) => carrera.id === this.carreraId())?.nombre,
  );
  protected readonly nombrePlan = computed(
    () => this.planes.value()?.find((plan) => plan.id === this.planId())?.nombre,
  );

  /**
   * Con un solo plan vigente, la lista de planes redirige a esta pantalla: volver ahí sería
   * rebotar. En ese caso el link vuelve a Carreras.
   */
  protected readonly volverAPlanes = computed(
    () => (this.planes.value() ?? []).filter((plan) => plan.vigente).length > 1,
  );
}
