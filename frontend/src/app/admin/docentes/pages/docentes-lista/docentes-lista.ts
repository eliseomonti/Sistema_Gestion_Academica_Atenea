import { Component, inject, linkedSignal, signal } from '@angular/core';
import { DocentesApi } from '../../data/docentes-api';
import { form, FormField } from '@angular/forms/signals';
import { rxResource } from '@angular/core/rxjs-interop';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { DocenteListadoResponse } from '../../data/docente';
import { Paginador } from '../../../../shared/ui/paginador/paginador';
import { Tabla } from '../../../../shared/ui/tabla/tabla';

@Component({
  imports: [FormField, Tabla, Paginador],
  selector: 'app-docentes-lista',
  styleUrl: './docentes-lista.css',
  templateUrl: './docentes-lista.html',
})
export class DocentesLista {
  private readonly api = inject(DocentesApi);
  private readonly filtros = signal({ buscar: '', estado: 'ACTIVOS' });
  protected readonly filtrosForm = form(this.filtros);

  protected readonly pagina = linkedSignal({ source: this.filtros, computation: () => 0 });

  protected readonly docentes = rxResource({
  params: () => ({
    buscar: this.filtros().buscar.trim(),
    activo: this.filtros().estado === 'ACTIVOS',
    page: this.pagina(),
  }),
  stream: ({ params }) => this.api.listarDocentes(params),
  });

  protected readonly columnas: Columna<DocenteListadoResponse>[] = [
    {
      clave: 'docente',
      titulo: 'Docente',
      valor: (d) => `${d.apellido}, ${d.nombre}`,
      principal: true,
    },
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    { clave: 'dni', titulo: 'DNI', tituloEnLista: true },
    { clave: 'email', titulo: 'Email' },
  ];




}
