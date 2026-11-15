import { Component, computed, inject, signal } from '@angular/core';
import { InscripcionFinalApi } from '../../data/inscripciones-final-api';
import { rxResource } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { InscripcionFinalResponse, OfertaFinalResponse } from '../../data/inscripcion-final';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';

@Component({
  imports: [DatePipe, ConfirmarDialog],
  selector: 'app-mesas',
  styleUrl: './mesas.css',
  templateUrl: './mesas.html',
})
export class Mesas {

  private readonly api = inject(InscripcionFinalApi);
  protected readonly oferta = rxResource({ stream: () => this.api.oferta()})
  protected readonly misInscripciones = rxResource({ stream: () => this.api.listar()})

  protected readonly mesas = computed(() => 
    [...(this.oferta.value() ?? [])].sort((a,b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
  )


  protected readonly inscribiendo = signal<number | null>(null)
  protected readonly aviso = signal<{examenFinalId: number; texto: string; esError: boolean} | null>(null)

  protected readonly aBajar =signal<InscripcionFinalResponse | null>(null)
  protected readonly bajando = signal(false)
  protected readonly avisoBaja = signal<{ texto: string; esError: boolean } | null>(null)

  protected horario(mesa: { horaInicio: string; horaFin: string; nombreAula: string | null }): string
  {
    return `${mesa.horaInicio.slice(0, 5)} a ${mesa.horaFin.slice(0, 5)} · ${mesa.nombreAula ?? 'Virtual'}`;
  }

   protected docentes(mesa: OfertaFinalResponse): string
  {
    if (mesa.docentes.length === 0) {
      return 'Tribunal a confirmar';
    }
    return mesa.docentes.map((d) => `${d.nombre.charAt(0)}. ${d.apellido}`).join(', ');
  }


  protected async inscribir(mesa: OfertaFinalResponse): Promise<void>
  {
    const id = mesa.examenFinalId;
    this.aviso.set(null);
    this.inscribiendo.set(id);
    try 
    {
      await firstValueFrom(this.api.inscribirte({ examenFinalId: id }));
      this.aviso.set({ examenFinalId: id, texto: 'Te inscribiste a la mesa de ' + mesa.nombreMateria, esError: false });
      this.oferta.reload();
      this.misInscripciones.reload();
    }catch(error)
    {
      this.aviso.set({ examenFinalId: id, texto: mensajeDeError(error), esError: true });
      if(error instanceof HttpErrorResponse && error.status === 409)
      {
        this.oferta.reload();
      }
    }
    this.inscribiendo.set(null);
  }

  protected mensajeBaja(inscripcion: InscripcionFinalResponse): string
  {
    return `Te vas a dar de baja de la mesa de ${inscripcion.nombreMateria}.`;
  }

  protected async darDeBaja(inscripcion: InscripcionFinalResponse): Promise<void>
  {
    this.avisoBaja.set(null);
    this.bajando.set(true);
    try {
      await firstValueFrom(this.api.darDeBaja(inscripcion.id));
      this.avisoBaja.set({ texto: 'Te diste de baja de la mesa de ' + inscripcion.nombreMateria, esError: false });
    } catch (error) {
      this.avisoBaja.set({ texto: mensajeDeError(error), esError: true });
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