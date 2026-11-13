import { Component, computed, inject } from '@angular/core';
import { OfertaComisionResponse, OfertaHorarioResponse, OfertaMateriaResponse, PeriodoLectivo } from '../../data/inscripcion-comision';
import { InscripcionesComisionApi } from '../../data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { DiaSemana } from '../../../../shared/models/dia-semana';



const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2do cuatrimestre',
  ANUAL: 'anual'
}

const DIAS: Record<DiaSemana, string> = {
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miercoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sabado',
  DOMINGO: 'Domingo'
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

  protected horario(h: OfertaHorarioResponse): string
  {
    const aula = h.aula ?? 'Virtual'
    return `${DIAS[h.diaSemana]} ${h.horaFin.slice(0,5)} a ${h.horaFin.slice(0,5)} en el aula ${aula}`;
  }

  protected docentes(comision: OfertaComisionResponse): string
  {
    if (comision.docentes.length === 0) {
      return 'Docentes a confirmar';
    }
    return comision.docentes
      .map((d) => `${d.rol === 'TITULAR' ? 'Titular' : 'Ayudante'}: ${d.nombre.charAt(0)} ${d.apellido}`).join(' · ');
  }

  protected cupo(comision: OfertaComisionResponse): string
  {
    if(comision.cupoDisponible === null)
    {
      return 'No hay limite de cupo en esta comision'
    }

    return comision.cupoDisponible === 1 ? 'Hay un lugar libre': `${comision.cupoDisponible} lugares libres `;
  }





}
