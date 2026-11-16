import { Component, inject, input, numberAttribute } from '@angular/core';
import { ExamenesFinalesApi } from '../../data/examen-final-api';
import { PeriodoInscripcionApi } from '../../../periodos-inscripcion/data/periodos-inscripcion-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { ExamenFinalResponse } from '../../data/examen-final';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { Celda } from '../../../../shared/ui/tabla/celda';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Icono } from '../../../../shared/ui/icono/icono';

@Component({
  imports: [Tabla, Celda, DatePipe, RouterLink, RouterOutlet, Icono],
  selector: 'app-mesas-lista',
  styleUrl: './mesas-lista.css',
  templateUrl: './mesas-lista.html',
})
export class MesasLista {


  private readonly api = inject(ExamenesFinalesApi);
  private readonly periodosApi = inject(PeriodoInscripcionApi);

  readonly periodoId = input.required({ transform: numberAttribute });

  protected readonly periodo = rxResource({
    params: () => this.periodoId(),
    stream: ({ params }) => this.periodosApi.obtenerPeriodo(params),
  });

  protected readonly mesas = rxResource({
    params: () => this.periodoId(),
    stream: ({ params }) => this.api.listar(params),
  });

  protected readonly columnas: Columna<ExamenFinalResponse>[] = [
    { clave: 'nombreMateria', titulo: 'Materia', principal: true },
    { clave: 'fecha', titulo: 'Fecha' },
    { clave: 'horario', titulo: 'Horario', valor: (m) => `${m.horaInicio.slice(0, 5)} a ${m.horaFin.slice(0, 5)}` },
    { clave: 'aula', titulo: 'Aula', valor: (m) => m.nombreAula ?? 'Virtual' },
    {
      clave: 'tribunal', titulo: 'Tribunal',
      valor: (m) => m.docentes.length === 0
        ? 'Sin asignar'
        : m.docentes.map((d) => `${d.nombre.charAt(0)}. ${d.apellido}`).join(', '),
    },
  ];

  protected readonly enlace = (mesa: ExamenFinalResponse) => [mesa.id];



}
