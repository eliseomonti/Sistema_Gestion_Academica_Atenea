import { Component, inject } from '@angular/core';
import { PeriodoLectivo } from '../../../inscripciones-comision/data/inscripcion-comision';
import { NotasApi } from '../../data/notas-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { CursadaEstudianteResponse, FinalEstudianteResponse, NotaEvaluacionEstudianteResponse } from '../../data/notas';
import { DatePipe } from '@angular/common';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';

const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2do cuatrimestre',
  ANUAL: 'Anual',
};

@Component({
  imports: [DatePipe, Etiqueta],
  selector: 'app-notas',
  styleUrl: './notas.css',
  templateUrl: './notas.html',
})
export class Notas {

  private readonly api = inject(NotasApi)
  protected readonly cursadas = rxResource({ stream: () => this.api.listarCursada() });
  protected readonly finales = rxResource({stream: () => this.api.listarFinales()})

  protected detalleCursada(cursada: CursadaEstudianteResponse): string
  {
    return `Comision ${cursada.nombreComision} ${cursada.anioLectivo} ${CUATRIMESTRES[cursada.periodoLectivo]}`
  }

  protected notaExamen(examen: NotaEvaluacionEstudianteResponse): string
  {
    if(examen.condicion === 'AUSENTE')
    {
      return 'Ausente';
    }

    return examen.nota !== null ? String(examen.nota): 'Sin nota';
  }

  protected condicionCursada(cursada: CursadaEstudianteResponse): {texto: string; tipo: 'ok' | 'error'} | null
  {
    switch(cursada.cierre?.condicion)
    {
      case 'PROMOCIONADO':
        return {texto: 'Promocionado', tipo: 'ok'};
        case 'REGULAR':
          return {texto: 'Regular', tipo: 'ok'};
          case 'NO_REGULAR':
            return {texto: 'Libre', tipo: 'error'};
            default: 
            return null;
    }
  }

  protected notaFinal(final: FinalEstudianteResponse): string
  {
    if(final.condicion === 'AUSENTE')
    {
      return 'Ausente'
    }

    return final.nota !== null ? String(final.nota) : 'Sin nota'
  }




}
