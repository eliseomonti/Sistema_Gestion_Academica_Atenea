import { Component, DestroyRef, inject, input, output } from '@angular/core';
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

  private espera?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
  }

  protected escribir(texto: string): void {
    clearTimeout(this.espera);
    this.espera = setTimeout(() => this.buscar.emit(texto), 300);
  }

}
