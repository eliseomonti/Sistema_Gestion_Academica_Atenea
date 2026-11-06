import { Component, input, output } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-paginador',
  styleUrl: './paginador.css',
  templateUrl: './paginador.html',
})
export class Paginador {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly cambio = output<number>();
  
}
