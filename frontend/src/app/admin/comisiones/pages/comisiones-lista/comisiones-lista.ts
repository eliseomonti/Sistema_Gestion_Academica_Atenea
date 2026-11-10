import { Component, inject, input, numberAttribute, signal } from '@angular/core';
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
  imports: [FormField,Tabla, Celda,TurnoPipe, RouterLink, RouterOutlet],
  selector: 'app-comisiones-lista',
  styleUrl: './comisiones-lista.css',
  templateUrl: './comisiones-lista.html',
})
export class ComisionesLista {

  private readonly api = inject(ComisionesAPi);
  readonly carreraId = input.required({transform: numberAttribute});
  readonly planId = input.required({transform: numberAttribute});
  protected readonly anios =[1,2,3,4,5,6,7]
  protected readonly turnos: Turno[] = ['MANANA', 'MEDIA_MANANA', 'TARDE', 'NOCHE']

  private readonly filtros = signal({
    anioLectivo: new Date().getFullYear(),
    anioCursado: '',
    turno: '',
    estado: 'ACTIVAS',
  });

  protected readonly filtrosForm = form(this.filtros)

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
    { clave: 'nombre', titulo: 'Comisión', principal: true },
    { clave: 'anioCursado', titulo: 'Año', valor: (c) => `${c.anioCursado}°`, tituloEnLista: true },
    { clave: 'turno', titulo: 'Turno' }, 
    { clave: 'cantidadMaterias', titulo: 'Materias', tituloEnLista: true },
  ];

  protected readonly enlace = (comision: ComisionResponse) => [comision.id];


}
