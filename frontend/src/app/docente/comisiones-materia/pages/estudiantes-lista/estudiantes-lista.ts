import { Component, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Columna } from '../../../../shared/ui/tabla/columna';
import { Tabla } from '../../../../shared/ui/tabla/tabla';
import { EstudianteCursadaResponse } from '../../data/comision-materia';
import { ComisionesMateriaApi } from '../../data/comisiones-materia-api';

/**
 * Los estudiantes anotados a la materia, que no se dieron de baja. El backend ya los devuelve
 * ordenados por apellido y nombre. Es la pestaña Estudiantes de ComisionMateriaDetalle.
 */
@Component({
  selector: 'app-estudiantes-lista',
  imports: [Tabla],
  templateUrl: './estudiantes-lista.html',
  styleUrl: './estudiantes-lista.css',
})
export class EstudiantesLista {
  private readonly api = inject(ComisionesMateriaApi);

  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  protected readonly estudiantes = rxResource({
    params: () => this.comisionMateriaId(),
    stream: ({ params: comisionMateriaId }) => this.api.listarEstudiantes(comisionMateriaId),
  });

  protected readonly columnas: Columna<EstudianteCursadaResponse>[] = [
    { clave: 'legajo', titulo: 'Legajo', tituloEnLista: true },
    {
      clave: 'nombre',
      titulo: 'Estudiante',
      valor: (e) => `${e.apellido}, ${e.nombre}`,
      principal: true,
    },
    { clave: 'email', titulo: 'Email' },
  ];
}
