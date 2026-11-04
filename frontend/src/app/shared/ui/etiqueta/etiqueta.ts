import { Component, input } from '@angular/core';

/**
 * Etiqueta de estado: verde si está bien, roja si no. El texto va adentro:
 *
 * <app-etiqueta tipo="ok">Activa</app-etiqueta>
 */
@Component({
  selector: 'app-etiqueta',
  templateUrl: './etiqueta.html',
  styleUrl: './etiqueta.css',
  host: { '[class]': 'tipo()' },
})
export class Etiqueta {
  readonly tipo = input.required<'ok' | 'error'>();
}
