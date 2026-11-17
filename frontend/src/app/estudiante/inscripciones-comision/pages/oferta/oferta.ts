import { Component, computed, inject, signal } from '@angular/core';
import { OfertaComisionResponse, OfertaHorarioResponse, OfertaMateriaResponse, PeriodoLectivo } from '../../data/inscripcion-comision';
import { InscripcionesComisionApi } from '../../data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { Icono } from '../../../../shared/ui/icono/icono';
import { Aviso } from '../../../../shared/ui/aviso/aviso';
import { DiaSemana } from '../../../../shared/models/dia-semana';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';



const CUATRIMESTRES: Record<PeriodoLectivo, string> = {
  PRIMER_CUATRIMESTRE: '1.er cuatrimestre',
  SEGUNDO_CUATRIMESTRE: '2.º cuatrimestre',
  ANUAL: 'Anual'
}

const DIAS: Record<DiaSemana, string> = {
  LUNES: 'Lun',
  MARTES: 'Mar',
  MIERCOLES: 'Mié',
  JUEVES: 'Jue',
  VIERNES: 'Vie',
  SABADO: 'Sáb',
  DOMINGO: 'Dom'
}

/** Con esta cantidad de lugares o menos, el cupo se resalta. */
const POCOS_LUGARES = 5;


@Component({
  imports: [TurnoPipe, Icono, Aviso],
  selector: 'app-oferta',
  styleUrl: './oferta.css',
  templateUrl: './oferta.html',
})
export class Oferta {


  private readonly api = inject(InscripcionesComisionApi);
  protected readonly inscribiendo = signal<number | null>(null);
  
  /** El error de la última inscripción, que se muestra en la fila de esa comisión. */
  protected readonly aviso = signal<{ ComisionMateriaId: number; texto: string; esError: boolean} | null>(null);
  /** El éxito se avisa con un cartel flotante. */
  protected readonly exito = signal<string | null>(null);


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

  /** "1.er", "2.º", "3.er"...: así se abrevian los ordinales en castellano. */
  protected ordinal(numero: number): string
  {
    return numero === 1 || numero === 3 ? `${numero}.er` : `${numero}.º`;
  }

  /** "Jue 08:30 a 10:30" */
  protected dia(h: OfertaHorarioResponse): string
  {
    return `${DIAS[h.diaSemana]} ${h.horaInicio.slice(0,5)} a ${h.horaFin.slice(0,5)}`;
  }

  /** "S. Moyano, R. Díaz" */
  protected docentes(comision: OfertaComisionResponse): string
  {
    if (comision.docentes.length === 0) {
      return 'Docentes a confirmar';
    }
    return comision.docentes.map((d) => `${d.nombre.charAt(0)}. ${d.apellido}`).join(', ');
  }

  protected cupo(comision: OfertaComisionResponse): string
  {
    const libres = comision.cupoDisponible;
    if (libres === null) {
      return 'Sin límite de cupo';
    }
    if (libres === 0) {
      return 'Sin cupo';
    }
    if (libres === 1) {
      return 'Queda 1 lugar';
    }
    return libres <= POCOS_LUGARES ? `Quedan ${libres} lugares` : `${libres} lugares libres`;
  }

  protected quedanPocos(comision: OfertaComisionResponse): boolean
  {
    return comision.cupoDisponible !== null && comision.cupoDisponible <= POCOS_LUGARES;
  }

  protected yaInscriptoEnMateria(materia: OfertaMateriaResponse): boolean
  {
    return materia.comisiones.some((c) => c.inscripto)
  }

  protected async inscribir(comision: OfertaComisionResponse): Promise<void>
  {
    const id = comision.comisionMateriaId;
    this.aviso.set(null)
    this.exito.set(null)
    this.inscribiendo.set(id);
    try{
      await firstValueFrom(this.api.inscribirse({comisionMateriaId: id}));
      this.exito.set('Te inscribiste a la comisión ' + comision.nombreComision)
      this.oferta.reload();
    }catch (error)
    {
      this.aviso.set({ComisionMateriaId: id, texto: mensajeDeError(error), esError: true})

      if(error instanceof HttpErrorResponse && error.status === 409)
      {
        this.oferta.reload()
      }
    }
    this.inscribiendo.set(null);
  }
}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}