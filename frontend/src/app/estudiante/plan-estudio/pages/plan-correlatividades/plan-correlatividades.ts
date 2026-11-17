import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { CorrelatividadesMateriaEstudianteResponse } from '../../data/plan-estudio';
import { PlanEstudioApi } from '../../data/plan-estudio-api';

type Fila = CorrelatividadesMateriaEstudianteResponse;

/** Una línea de una celda: "Cursadas" con sus códigos. */
interface Exigencia {
  etiqueta: string;
  codigos: string[];
}

/** "1.er", "2.º", "3.er"...: así se abrevian los ordinales en castellano. */
function ordinal(numero: number): string {
  return numero === 1 || numero === 3 ? `${numero}.er` : `${numero}.º`;
}

/**
 * El régimen de correlatividades de cada plan en el que está inscripto el estudiante: para cada
 * materia, los códigos de las que tiene que tener cursadas o aprobadas para cursarla y para
 * rendirla. Es la pestaña Correlatividades de la pantalla Plan de estudio.
 */
@Component({
  selector: 'app-plan-correlatividades',
  imports: [Tabla, Celda],
  templateUrl: './plan-correlatividades.html',
  styleUrl: './plan-correlatividades.css',
})
export class PlanCorrelatividades {
  private readonly api = inject(PlanEstudioApi);

  /** Primero los planes, para saber sus ids, y después las correlatividades de cada uno. */
  protected readonly planes = rxResource({
    stream: () =>
      this.api.listarPlan().pipe(
        switchMap((planes) =>
          planes.length === 0
            ? of([])
            : forkJoin(
                planes.map((plan) =>
                  this.api
                    .listarCorrelatividades(plan.planEstudioId)
                    .pipe(map((filas) => ({ plan, filas }))),
                ),
              ),
        ),
      ),
  });

  protected readonly columnas: Columna<Fila>[] = [
    { clave: 'codigo', titulo: 'Código', tituloEnLista: true }, // <ng-template appCelda>
    { clave: 'nombre', titulo: 'Materia', principal: true },
    { clave: 'paraCursar', titulo: 'Para cursar', tituloEnLista: true }, // <ng-template appCelda>
    { clave: 'paraRendir', titulo: 'Para rendir', tituloEnLista: true }, // <ng-template appCelda>
  ];

  /** Las líneas de una celda, sin las vacías. Si no exige nada, ninguna: la celda muestra una raya. */
  protected exigencias(cursadas: string[], aprobadas: string[]): Exigencia[] {
    return [
      { etiqueta: 'Cursadas', codigos: cursadas },
      { etiqueta: 'Aprobadas', codigos: aprobadas },
    ].filter((exigencia) => exigencia.codigos.length > 0);
  }

  /** "1.er año" */
  protected readonly grupo = (fila: Fila) => `${ordinal(fila.anioCursado)} año`;

  protected readonly cantidad = (n: number) => (n === 1 ? '1 materia' : `${n} materias`);
}
