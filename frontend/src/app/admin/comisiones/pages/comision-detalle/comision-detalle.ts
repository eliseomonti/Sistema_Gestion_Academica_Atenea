import { Component, inject, input, numberAttribute, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { PeriodoLectivo } from '../../../../shared/models/periodo-lectivo';
import { Cuatrimestre, GrillaHoraria } from '../../components/grilla-horaria/grilla-horaria';
import { ComisionMateriaResponse } from '../../data/comision';
import { ComisionesAPi } from '../../data/comisiones-api';

const PERIODOS: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1.er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2.º cuatrimestre',
  ANUAL: 'Anual',
};

/** "Apellido, Nombre" del titular, o "Sin asignar" si la materia todavía no tiene. */
function titular(materia: ComisionMateriaResponse): string {
  const docente = materia.docentes.find((d) => d.rol === 'TITULAR');
  return docente ? `${docente.apellido}, ${docente.nombre}` : 'Sin asignar';
}

/**
 * Una comisión con lo que se cursa en ella. Va en la pestaña Comisiones del plan (PlanDetalle),
 * en lugar de la lista. Las ventanas (editar la comisión) se abren en su <router-outlet>.
 */
@Component({
  selector: 'app-comision-detalle',
  imports: [RouterLink, RouterOutlet, FormField, Icono, Tabla, TurnoPipe, GrillaHoraria],
  templateUrl: './comision-detalle.html',
  styleUrl: './comision-detalle.css',
})
export class ComisionDetalle {
  private readonly api = inject(ComisionesAPi);

  readonly comisionId = input.required({ transform: numberAttribute });

  protected readonly comision = rxResource({
    params: () => this.comisionId(),
    stream: ({ params: comisionId }) => this.api.obtenerComision(comisionId),
  });

  /** Qué cuatrimestre muestra la grilla. */
  protected readonly vista = signal<{ cuatrimestre: Cuatrimestre }>({
    cuatrimestre: 'PRIMER_CUATRIMESTRE',
  });
  protected readonly vistaForm = form(this.vista);

  protected readonly columnas: Columna<ComisionMateriaResponse>[] = [
    { clave: 'nombreMateria', titulo: 'Materia', principal: true },
    { clave: 'periodoLectivo', titulo: 'Período', valor: (m) => PERIODOS[m.periodoLectivo] },
    { clave: 'titular', titulo: 'Titular', valor: titular },
    {
      clave: 'cupoMaximo',
      titulo: 'Cupo',
      valor: (m) => m.cupoMaximo ?? 'Sin límite',
      tituloEnLista: true,
    },
  ];

  /** Cada fila abre la ventana para editar o quitar esa materia. */
  protected readonly enlace = (materia: ComisionMateriaResponse) => ['materias', materia.id];
}
