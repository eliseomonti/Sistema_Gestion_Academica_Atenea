import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

/** Una pestaña: `ruta` es relativa a la pantalla que la muestra ('materias'). */
export interface Pestana {
  texto: string;
  ruta: string;
}

/**
 * Una fila de pestañas, cada una con su ruta hija. La pantalla que las usa pone su
 * <router-outlet> debajo:
 *
 * <app-pestanas [pestanas]="pestanas" etiqueta="Secciones del plan" />
 */
@Component({
  selector: 'app-pestanas',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './pestanas.html',
  styleUrl: './pestanas.css',
})
export class Pestanas {
  readonly pestanas = input.required<Pestana[]>();
  /** El nombre del grupo de pestañas, para los lectores de pantalla. */
  readonly etiqueta = input.required<string>();
}
