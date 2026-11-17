import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';
import { PeriodoLectivo } from '../../../../shared/models/periodo-lectivo';
import { PeriodoInscripcionApi } from '../../data/periodos-inscripcion-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField } from '@angular/forms/signals';
import { PeriodoInscripcionResponse, TipoPeriodo } from '../../data/periodo-inscripcion';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';


const CUATRIMESTRE: Record<PeriodoLectivo, string> =
{
  PRIMER_CUATRIMESTRE: '1er Cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2do Cuatrimestre',
  ANUAL: 'Anual',
};


@Component({
  imports: [FormField, Tabla, Celda, DatePipe, RouterLink, RouterOutlet, Icono, Etiqueta],
  selector: 'app-periodos-lista',
  styleUrl: './periodos-lista.css',
  templateUrl: './periodos-lista.html',
})
export class PeriodosLista {

  private readonly api = inject(PeriodoInscripcionApi);

  readonly carreraId = input.required({transform: numberAttribute})

  private readonly filtros = signal({
    anioLectivo: new Date().getFullYear(),
    tipo: '',
    estado: 'TODOS',
  });

  protected readonly filtrosForm = form(this.filtros);

  protected readonly hayFiltro = computed(() => {
    const filtros = this.filtros();
    return filtros.anioLectivo !== new Date().getFullYear() || filtros.tipo !== '' || filtros.estado !== 'TODOS';
  });

  protected readonly periodosFiltrados = computed(() => {
    const estado = this.filtros().estado;
    return (this.periodos.value() ?? []).filter(
      (p) => estado === 'TODOS' || p.habilitada === (estado === 'HABILITADOS'),
    );
  });

  protected readonly periodos = rxResource({
    params: () => ({
      carreraId: this.carreraId(),
      anioLectivo: this.filtros().anioLectivo || undefined,
      tipo: this.filtros().tipo === '' ? undefined : (this.filtros().tipo as TipoPeriodo), 
    }),
    stream: ({params}) => this.api.listar(params)
  });


  protected readonly columnas: Columna<PeriodoInscripcionResponse>[] = [
    {clave: 'tipo', titulo: 'Tipo', valor: (p) => (p.tipo === 'CURSADA' ? 'Cursada' : 'Final'), principal: true},

    {clave: 'periodoLectivo', titulo: 'Cuatrimestre', valor: (p) => (p.periodoLectivo ? CUATRIMESTRE[p.periodoLectivo]: '-')},

    {clave: 'fechaInicio', titulo: 'Desde'},

    {clave: 'fechaCierre', titulo: 'Hasta'},
    
    {clave: 'habilitada', titulo: 'Estado'},

    {clave: 'mesas', titulo: 'Mesas'}

    
  
  ];

  protected readonly enlace = (periodo: PeriodoInscripcionResponse) => [periodo.id];


}
