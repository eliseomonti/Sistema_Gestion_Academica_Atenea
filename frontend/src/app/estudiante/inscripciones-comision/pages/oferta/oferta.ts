import { Component, computed, inject, signal } from '@angular/core';
import { InscripcionComisionResponse, OfertaComisionResponse, OfertaHorarioResponse, OfertaMateriaResponse, PeriodoLectivo } from '../../data/inscripcion-comision';
import { InscripcionesComisionApi } from '../../data/inscripciones-comision-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { TurnoPipe } from '../../../../shared/pipes/turno-pipe';
import { DiaSemana } from '../../../../shared/models/dia-semana';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { DatePipe } from '@angular/common';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';



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
  imports: [TurnoPipe,DatePipe, ConfirmarDialog],
  selector: 'app-oferta',
  styleUrl: './oferta.css',
  templateUrl: './oferta.html',
})
export class Oferta {


  private readonly api = inject(InscripcionesComisionApi);
  protected readonly inscribiendo = signal<number | null>(null);
  
  protected readonly aviso = signal<{ ComisionMateriaId: number; texto: string; esError: boolean} | null>(null);


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


  protected yaInscriptoEnMateria(materia: OfertaMateriaResponse): boolean
  {
    return materia.comisiones.some((c) => c.inscripto)
  }

  protected async inscribir(comision: OfertaComisionResponse): Promise<void>
  {
    const id = comision.comisionMateriaId;
    this.aviso.set(null)
    this.inscribiendo.set(id);
    try{
      await firstValueFrom(this.api.inscribirse({comisionMateriaId: id}));
      this.aviso.set({ComisionMateriaId: id, texto: 'Se inscribio a la comision' + comision.nombreComision, esError: false})
      this.oferta.reload();
      this.misInscripciones.reload();
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

  protected readonly misInscripciones = rxResource({
    stream: () => this.api.listarInscripciones(),
  })

  protected readonly aBajar = signal<InscripcionComisionResponse | null>(null)
  protected readonly bajando = signal(false)
  protected readonly avisoBaja = signal<{texto: string, esError: boolean} | null>(null)

   protected mensajeBaja(inscripcion: InscripcionComisionResponse): string
  {
    return `Te vas a dar de baja de ${inscripcion.nombreMateria}, comisión ${inscripcion.nombreComision}.`;
  }

  protected async darDeBaja(inscripcion: InscripcionComisionResponse): Promise<void>
  {
    this.avisoBaja.set(null);
    this.bajando.set(true);
    try
    {
      await firstValueFrom(this.api.darDeBaja(inscripcion.id));
      this.avisoBaja.set({ texto: 'Te diste de baja de ' + inscripcion.nombreMateria, esError: false });
    }catch(error)
    {
      this.avisoBaja.set({texto: mensajeDeError(error),esError: true})
    }
    this.aBajar.set(null);
    this.bajando.set(false);
    this.misInscripciones.reload();
    this.oferta.reload();
  }






}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}