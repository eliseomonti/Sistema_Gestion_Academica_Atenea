import { NgTemplateOutlet } from '@angular/common';
import {
  booleanAttribute,
  Component,
  computed,
  contentChildren,
  input,
  TemplateRef,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icono } from '../icono/icono';
import { Celda } from './celda';
import { Columna } from './columna';

/** Filas seguidas con el mismo subtítulo dentro de un grupo. Sin `subagrupar`, una sola, sin título. */
interface Subgrupo<T> {
  titulo: string | null;
  filas: T[];
}

/** Filas seguidas que comparten grupo. Sin `agrupar`, hay un solo grupo, sin título. */
interface Grupo<T> {
  titulo: string | null;
  cantidad: number;
  subgrupos: Subgrupo<T>[];
}

/**
 * La tabla de todos los listados. La pantalla pasa las columnas y las filas, y la tabla arma el
 * encabezado y las celdas. En un espacio angosto (celular), cada fila se muestra como una tarjeta.
 *
 * - Con `enlace`, toda la fila lleva a otra pantalla y termina con una flecha.
 * - Con `agrupar`, las filas se separan en grupos con una franja de título ("1.er año").
 * - Con `[encabezado]="false"`, no se muestra la fila de títulos de columna.
 */
@Component({
  selector: 'app-tabla',
  imports: [NgTemplateOutlet, RouterLink, Icono],
  templateUrl: './tabla.html',
  styleUrl: './tabla.css',
  host: { '[class.centrado]': 'centrado()' },
})
export class Tabla<T> {
  readonly columnas = input.required<Columna<T>[]>();
  readonly filas = input.required<T[]>();
  /** A dónde lleva cada fila, con el formato de `routerLink`. Por ejemplo `(c) => [c.id, 'planes']`. */
  readonly enlace = input<(fila: T) => unknown[]>();
  /**
   * Título del grupo de cada fila. Las filas tienen que venir ordenadas por grupo: se agrupan las
   * que están seguidas y tienen el mismo título.
   */
  readonly agrupar = input<(fila: T) => string>();
  /** Texto que acompaña el título del grupo y del subgrupo, según su cantidad de filas. */
  readonly detalleGrupo = input<(cantidad: number) => string>();
  /**
   * Sólo en la lista de celular: separa cada grupo en tandas con una franja de subtítulo
   * ("1.er cuatrimestre"). Como `agrupar`, las filas tienen que venir ordenadas. En escritorio ese
   * dato va en su columna, que en celular se oculta con `ocultaEnLista`.
   */
  readonly subagrupar = input<(fila: T) => string>();
  /** Si se muestra la fila de títulos de columna. Con una sola columna, a veces sobra. */
  readonly encabezado = input(true);
  /** PRUEBA: en escritorio, todo centrado menos la columna principal. `<app-tabla centrado>`. */
  readonly centrado = input(false, { transform: booleanAttribute });

  /** Los `<ng-template appCelda>` que puso la pantalla adentro de `<app-tabla>`. */
  private readonly celdas = contentChildren(Celda);

  /** Template de cada columna especial, buscado por su clave. */
  protected readonly plantillas = computed(
    () => new Map<string, TemplateRef<unknown>>(this.celdas().map((c) => [c.clave(), c.template])),
  );

  /** La columna que nombra a la fila: la marcada como `principal`, o la primera. */
  protected readonly columnaPrincipal = computed(
    () => this.columnas().find((columna) => columna.principal) ?? this.columnas()[0],
  );

  /** Para que la franja de un grupo ocupe todo el ancho, flecha incluida. */
  protected readonly cantidadColumnas = computed(
    () => this.columnas().length + (this.enlace() ? 1 : 0),
  );

  protected readonly grupos = computed<Grupo<T>[]>(() => {
    const agrupar = this.agrupar();
    const subagrupar = this.subagrupar();
    const grupos: Grupo<T>[] = [];
    for (const fila of this.filas()) {
      const titulo = agrupar ? agrupar(fila) : null;
      let grupo = grupos.at(-1);
      if (!grupo || grupo.titulo !== titulo) {
        grupo = { titulo, cantidad: 0, subgrupos: [] };
        grupos.push(grupo);
      }
      const subtitulo = subagrupar ? subagrupar(fila) : null;
      let subgrupo = grupo.subgrupos.at(-1);
      if (!subgrupo || subgrupo.titulo !== subtitulo) {
        subgrupo = { titulo: subtitulo, filas: [] };
        grupo.subgrupos.push(subgrupo);
      }
      subgrupo.filas.push(fila);
      grupo.cantidad++;
    }
    return grupos;
  });

  protected valor(fila: T, columna: Columna<T>): string | number {
    if (columna.valor) {
      return columna.valor(fila);
    }
    const dato = (fila as Record<string, unknown>)[columna.clave];
    return dato === null || dato === undefined ? '' : String(dato);
  }
}
