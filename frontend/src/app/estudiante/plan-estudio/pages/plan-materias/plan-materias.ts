import { Component, inject } from '@angular/core';
import { PeriodoLectivo } from '../../../inscripciones-comision/data/inscripcion-comision';
import { EstadoMateria, MateriaPlanEstudianteResponse, PlanEstudianteResponse } from '../../data/plan-estudio';
import { PlanEstudioApi } from '../../data/plan-estudio-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { Etiqueta } from '../../../../shared/ui/etiqueta/etiqueta';


const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2do cuatrimestre',
  ANUAL: 'Anual',
};

const ESTADOS: Record<EstadoMateria, { texto: string; tipo: 'ok' | 'error' | null }> = {
  APROBADA: { texto: 'Aprobada', tipo: 'ok' },
  REGULAR: { texto: 'Regular', tipo: null },
  CURSANDO: { texto: 'Cursando', tipo: null },
  REGULARIDAD_VENCIDA: { texto: 'Regularidad vencida', tipo: 'error' },
  NO_REGULAR: { texto: 'No regular', tipo: 'error' },
  NO_CURSADA: { texto: 'Sin cursar', tipo: null },
};

@Component({
  imports: [Etiqueta],
  selector: 'app-plan-materias',
  styleUrl: './plan-materias.css',
  templateUrl: './plan-materias.html',
})
export class PlanMaterias {

  private readonly api = inject(PlanEstudioApi)

  protected readonly planes = rxResource({
    stream: () => this.api.listarPlan()
  })

  protected aniosDe(plan: PlanEstudianteResponse): number[]
  {
    return [...new Set(plan.materias.map((m)=> m.anioCursado))].sort((a,b)=> a-b)
  }

  protected materiasDe(plan:PlanEstudianteResponse, anio: number): MateriaPlanEstudianteResponse[]
  {
    return plan.materias.filter((m) => m.anioCursado === anio)
  }

  protected cuatrimestre(periodo: PeriodoLectivo): string
  {
    return CUATRIMESTRES[periodo]
  }

  protected estado(materia: MateriaPlanEstudianteResponse)
  {
    return ESTADOS[materia.estado]
  }

}
