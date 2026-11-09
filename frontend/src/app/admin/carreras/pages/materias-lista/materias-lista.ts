import { Component, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { PeriodoLectivo, PlanMateriaResponse } from '../../data/carrera';
import { CarrerasApi } from '../../data/carreras-api';

const PERIODOS: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1.er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2.º cuatrimestre',
  ANUAL: 'Anual',
};

/** "1.er", "2.º", "3.er"...: así se abrevian los ordinales en castellano. */
function ordinal(numero: number): string {
  return numero === 1 || numero === 3 ? `${numero}.er` : `${numero}.º`;
}

/**
 * Las materias de un plan de estudio, agrupadas por año. El backend ya las devuelve ordenadas por
 * año, período y nombre, que es lo que necesita la tabla para agruparlas. Es la pestaña Materias
 * de la pantalla del plan (PlanDetalle).
 */
@Component({
  selector: 'app-materias-lista',
  imports: [Tabla, Celda],
  templateUrl: './materias-lista.html',
  styleUrl: './materias-lista.css',
})
export class MateriasLista {
  private readonly api = inject(CarrerasApi);

  readonly carreraId = input.required({ transform: numberAttribute });
  readonly planId = input.required({ transform: numberAttribute });

  /** Se vuelve a pedir si cambia la carrera o el plan de la URL. */
  protected readonly materias = rxResource({
    params: () => ({ carreraId: this.carreraId(), planId: this.planId() }),
    stream: ({ params }) => this.api.listarMaterias(params.carreraId, params.planId),
  });

  /** En celular, el título de la fila es la materia y el código va debajo. */
  protected readonly columnas: Columna<PlanMateriaResponse>[] = [
    { clave: 'nombre', titulo: 'Materia', principal: true },
    // En celular, el período va como subtítulo (`subagrupar`) en vez de en cada fila.
    { clave: 'periodoLectivo', titulo: 'Período', ocultaEnLista: true }, // <ng-template appCelda>
    { clave: 'codigo', titulo: 'Código', tituloEnLista: true }, // lo muestra <ng-template appCelda>
  ];

  /** "1.er cuatrimestre" */
  protected readonly periodo = (materia: PlanMateriaResponse) => PERIODOS[materia.periodoLectivo];

  /** "4.º año" */
  protected readonly grupo = (materia: PlanMateriaResponse) =>
    `${ordinal(materia.anioCursado)} año`;

  protected readonly cantidad = (n: number) => (n === 1 ? '1 materia' : `${n} materias`);
}
