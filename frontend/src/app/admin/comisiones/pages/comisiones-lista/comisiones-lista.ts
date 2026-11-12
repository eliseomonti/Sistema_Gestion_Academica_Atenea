import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { Icono } from '../../../../shared/ui/icono/icono';
import { CarrerasApi } from '../../../carreras/data/carreras-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { ComisionResponse } from '../../data/comision';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { ComisionesAPi } from '../../data/comisiones-api';
import { Turno } from '../../../../shared/models/turno';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { RouterLink, RouterOutlet } from '@angular/router';


@Component({
  imports: [FormField, Tabla, Celda, TurnoPipe, RouterLink, RouterOutlet, Icono],
  selector: 'app-comisiones-lista',
  styleUrl: './comisiones-lista.css',
  templateUrl: './comisiones-lista.html',
})
export class ComisionesLista {

  private readonly api = inject(ComisionesAPi);
  private readonly carrerasApi = inject(CarrerasApi);
  readonly carreraId = input.required({transform: numberAttribute});
  readonly planId = input.required({transform: numberAttribute});
  protected readonly turnos: Turno[] = ['MANANA', 'MEDIA_MANANA', 'TARDE', 'NOCHE']

  private readonly materias = rxResource({
    params: () => ({ carreraId: this.carreraId(), planId: this.planId() }),
    stream: ({ params }) => this.carrerasApi.listarMaterias(params.carreraId, params.planId),
  });

  /**
   * Los años de la carrera. El plan no dice cuántos años dura, así que salen de sus materias:
   * sólo los años que tienen alguna, ordenados. Igual que en la ventana de alta.
   */
  protected readonly anios = computed(() =>
    [...new Set((this.materias.value() ?? []).map((m) => m.anioCursado))].sort((a, b) => a - b),
  );

  private readonly anioActual = new Date().getFullYear();

  private readonly filtros = signal({
    anioLectivo: this.anioActual,
    anioCursado: '',
    turno: '',
    estado: 'ACTIVAS',
  });

  protected readonly filtrosForm = form(this.filtros)

  /** Algún filtro distinto del de siempre (este año, todos los años y turnos, activas). */
  protected readonly hayFiltro = computed(() => {
    const filtros = this.filtros();
    return (
      filtros.anioLectivo !== this.anioActual ||
      filtros.anioCursado !== '' ||
      filtros.turno !== '' ||
      filtros.estado !== 'ACTIVAS'
    );
  });

  protected readonly comisiones = rxResource({
    params: () => ({
      carreraId: this.carreraId(),
      planEstudioId: this.planId(),
      anioLectivo: this.filtros().anioLectivo || undefined,
      anioCursado: this.filtros().anioCursado === '' ? undefined : Number(this.filtros().anioCursado),
      turno: this.filtros().turno === '' ? undefined : (this.filtros().turno as Turno),
      activa: this.filtros().estado === 'ACTIVAS',
    }),
    stream: ({ params }) => this.api.listar(params),
  });

  protected readonly columnas: Columna<ComisionResponse>[] = [
    { clave: 'nroComision', titulo: 'Comisión', principal: true },
    { clave: 'anioLectivo', titulo: 'Año lectivo', tituloEnLista: true },
    {
      clave: 'anioCursado',
      titulo: 'Año de la carrera',
      valor: (c) => `${c.anioCursado}°`,
      tituloEnLista: true,
    },
    { clave: 'turno', titulo: 'Turno' },
  ];

  protected readonly enlace = (comision: ComisionResponse) => [comision.id];


}
