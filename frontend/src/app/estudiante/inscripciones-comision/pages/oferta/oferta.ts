import { Component, computed, inject } from '@angular/core';
import { OfertaMateriaResponse, PeriodoLectivo } from '../../data/inscripcion-comision';
import { InscripcionesComisionApi } from '../../data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';



const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2do cuatrimestre',
  ANUAL: 'anual'
}


@Component({
  imports: [TurnoPipe],
  selector: 'app-oferta',
  styleUrl: './oferta.css',
  templateUrl: './oferta.html',
})
export class Oferta {


  private readonly api = inject(InscripcionesComisionApi);

  protected readonly oferta = rxResource({
    stream: () => this.api.disponibilidad()
  })

  protected readonly anio = computed(() => {
    const materias = this.oferta.value() ?? [];
    const anios = materias.map((m)=> m.anioCursado)
    return [... new Set(anios)].sort((a,b) => a - b)
  })

  protected materiasDelAnio(anio: number): OfertaMateriaResponse[]
  {
    return (this.oferta.value() ?? []).filter((m) => m.anioCursado === anio)
  }

  protected cuatrimestre(periodo: PeriodoLectivo): string
  {
    return CUATRIMESTRES[periodo];
  }





}
