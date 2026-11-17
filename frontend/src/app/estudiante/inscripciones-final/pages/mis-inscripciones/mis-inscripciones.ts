import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { InscripcionFinalResponse } from '../../data/inscripcion-final';
import { InscripcionFinalApi } from '../../data/inscripciones-final-api';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Aviso } from '../../../../shared/ui/aviso/aviso';

/** Las inscripciones a mesas de final del estudiante, con la baja. Es una pestaña de Inscripción a finales. */
@Component({
  imports: [DatePipe, ConfirmarDialog, Aviso],
  selector: 'app-mis-inscripciones-final',
  styleUrl: './mis-inscripciones.css',
  templateUrl: './mis-inscripciones.html',
})
export class MisInscripcionesFinal {

  private readonly api = inject(InscripcionFinalApi);
  protected readonly misInscripciones = rxResource({ stream: () => this.api.listar()})

  protected readonly aBajar =signal<InscripcionFinalResponse | null>(null)
  protected readonly bajando = signal(false)
  protected readonly avisoBaja = signal<{ inscripcionId: number; texto: string; esError: boolean } | null>(null)

  protected horario(mesa: { horaInicio: string; horaFin: string; nombreAula: string | null }): string
  {
    return `${mesa.horaInicio.slice(0, 5)} a ${mesa.horaFin.slice(0, 5)} · ${mesa.nombreAula ?? 'Virtual'}`;
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
      this.avisoBaja.set({ inscripcionId: inscripcion.id, texto: 'Te diste de baja de la mesa de ' + inscripcion.nombreMateria, esError: false });
    } catch (error) {
      this.avisoBaja.set({ inscripcionId: inscripcion.id, texto: mensajeDeError(error), esError: true });
    }
    this.aBajar.set(null);
    this.bajando.set(false);
    this.misInscripciones.reload();
  }


}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}