import { Component, inject } from '@angular/core';
import { Sesion } from '../../../../core/seguridad/sesion';
import { Bienvenida } from '../../../../shared/ui/bienvenida/bienvenida';

/** La pantalla a la que se entra después del login. Por ahora, sólo la bienvenida. */
@Component({
  selector: 'app-inicio',
  imports: [Bienvenida],
  templateUrl: './inicio.html',
})
export class Inicio {
  protected readonly sesion = inject(Sesion);
}
