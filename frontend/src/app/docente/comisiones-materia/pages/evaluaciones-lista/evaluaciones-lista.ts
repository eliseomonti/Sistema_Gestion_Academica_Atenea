import { Component, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { EvaluacionResponse, TIPOS_EVALUACION } from '../../data/comision-materia';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

/**
 * Las evaluaciones de la materia (parciales, trabajos prácticos, recuperatorios...). El backend ya
 * las devuelve ordenadas por fecha. Es la pestaña Evaluaciones de ComisionMateriaDetalle.
 */
@Component({
  selector: 'app-evaluaciones-lista',
  imports: [RouterLink, RouterOutlet, Tabla],
  templateUrl: './evaluaciones-lista.html',
  styleUrl: './evaluaciones-lista.css',
})
export class EvaluacionesLista {
  private readonly api = inject(ComisionesMateriaApi);

  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  protected readonly evaluaciones = rxResource({
    params: () => this.comisionMateriaId(),
    stream: ({ params: comisionMateriaId }) => this.api.listarEvaluaciones(comisionMateriaId),
  });

  protected readonly columnas: Columna<EvaluacionResponse>[] = [
    { clave: 'fecha', titulo: 'Fecha', valor: (e) => ddmmaaaa(e.fecha), tituloEnLista: true },
    { clave: 'nombre', titulo: 'Evaluación', principal: true },
    { clave: 'tipo', titulo: 'Tipo', valor: (e) => this.tipo(e) },
  ];

  /** Cada fila abre la planilla de notas de la evaluación. */
  protected readonly enlace = (evaluacion: EvaluacionResponse) => [evaluacion.id];

  /** "Parcial", o "Recuperatorio de Parcial 1": la que recupera está en la misma lista. */
  private tipo(evaluacion: EvaluacionResponse): string {
    if (evaluacion.evaluacionRecuperadaId === null) {
      return TIPOS_EVALUACION[evaluacion.tipo];
    }
    const recuperada = this.evaluaciones
      .value()
      ?.find((otra) => otra.id === evaluacion.evaluacionRecuperadaId);
    return recuperada ? `Recuperatorio de ${recuperada.nombre}` : 'Recuperatorio';
  }
}

/** "2026-05-14" → "14/05/2026". */
function ddmmaaaa(fecha: string): string {
  return fecha.split('-').reverse().join('/');
}
