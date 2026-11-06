import { Component, input, output } from '@angular/core';
import { DocenteListadoResponse } from '../../../docentes/data/docente';

@Component({
  imports: [],
  selector: 'app-docente-buscador',
  styleUrl: './docente-buscador.css',
  templateUrl: './docente-buscador.html',
})
export class DocenteBuscador {

  readonly opcion = input.required<DocenteListadoResponse[]>();
  readonly buscar = output<string>();
  readonly elegir = output<DocenteListadoResponse>();


}
