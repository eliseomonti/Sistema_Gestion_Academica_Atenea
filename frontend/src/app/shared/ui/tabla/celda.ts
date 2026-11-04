import { Directive, inject, input, TemplateRef } from '@angular/core';

/**
 * Celda especial de `app-tabla`: en vez del texto de la columna, muestra el HTML del template.
 * La fila llega como `let-nombre`:
 *
 * <ng-template appCelda="activa" let-carrera>...</ng-template>
 */
@Directive({ selector: 'ng-template[appCelda]' })
export class Celda {
  /** La `clave` de la columna que reemplaza. */
  readonly clave = input.required<string>({ alias: 'appCelda' });
  readonly template = inject(TemplateRef);
}
