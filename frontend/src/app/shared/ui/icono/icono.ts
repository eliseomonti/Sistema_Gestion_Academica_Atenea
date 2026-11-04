import { Component, input } from '@angular/core';

/** Los íconos disponibles. Para sumar uno, se agrega acá y su dibujo en `icono.html`. */
export type NombreIcono =
  | 'menu'
  | 'cerrar'
  | 'filtro'
  | 'flecha-derecha'
  | 'flecha-izquierda'
  | 'ojo'
  | 'ojo-tachado'
  | 'birrete'
  | 'persona'
  | 'libro'
  | 'capas'
  | 'documento'
  | 'calendario'
  | 'editar'
  | 'portapapeles'
  | 'medalla';

/**
 * Ícono de Feather Icons (MIT), salvo `birrete`, que es de Lucide (ISC) y tiene el mismo diseño.
 * Es decorativo: los lectores de pantalla lo saltean, así que el texto va en el `aria-label` del
 * botón o link que lo contiene.
 *
 * <app-icono nombre="filtro" />
 *
 * Mide 1.25rem y toma el color del texto. Para otro tamaño, el componente que lo usa le cambia
 * `width` y `height` desde su CSS: `.hamburguesa app-icono { width: 1.5rem; height: 1.5rem; }`.
 */
@Component({
  selector: 'app-icono',
  templateUrl: './icono.html',
  styleUrl: './icono.css',
  host: { 'aria-hidden': 'true' },
})
export class Icono {
  readonly nombre = input.required<NombreIcono>();
}
