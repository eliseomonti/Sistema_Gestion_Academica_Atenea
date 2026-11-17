import { Component, DestroyRef, inject, output } from '@angular/core';
import { Icono } from '../icono/icono';

/** Cuánto queda en pantalla antes de cerrarse solo. */
const DURACION_MS = 5000;

/**
 * Cartel flotante para avisar que algo salió bien, abajo y al centro de la pantalla: se ve
 * aunque la acción haya quedado lejos. Se cierra solo o con la X; la pantalla lo saca de su @if
 * cuando recibe `cerrar`. El texto va adentro:
 *
 * <app-aviso (cerrar)="exito.set(null)">Te diste de baja de Matemática</app-aviso>
 */
@Component({
  selector: 'app-aviso',
  imports: [Icono],
  templateUrl: './aviso.html',
  styleUrl: './aviso.css',
  host: { role: 'status' },
})
export class Aviso {
  readonly cerrar = output();

  constructor() {
    const temporizador = setTimeout(() => this.cerrar.emit(), DURACION_MS);
    inject(DestroyRef).onDestroy(() => clearTimeout(temporizador));
  }
}
