import { Component, input, output } from '@angular/core';
import { Icono } from '../icono/icono';

/**
 * Botones de página anterior y siguiente de un listado paginado. No pide datos: avisa qué página
 * se eligió y la pantalla la vuelve a pedir al backend. Con una sola página no muestra nada.
 *
 * <app-paginador [pagina]="pagina()" [totalPaginas]="total" (cambiar)="irA($event)" />
 */
@Component({
  selector: 'app-paginador',
  imports: [Icono],
  templateUrl: './paginador.html',
  styleUrl: './paginador.css',
})
export class Paginador {
  /** La página que se está mostrando. Empieza en 0, como en el backend. */
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();

  /** La página nueva, también empezando en 0. */
  readonly cambiar = output<number>();
}
