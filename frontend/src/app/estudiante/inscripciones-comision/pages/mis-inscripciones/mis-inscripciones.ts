import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { InscripcionComisionResponse } from '../../data/inscripcion-comision';
import { InscripcionesComisionApi } from '../../data/inscripciones-comision-api';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Aviso } from '../../../../shared/ui/aviso/aviso';

/** Las inscripciones a comisión del estudiante, con la baja. Es una pestaña de Inscripción a cursado. */
@Component({
  imports: [DatePipe, ConfirmarDialog, Aviso],
  selector: 'app-mis-inscripciones',
  styleUrl: './mis-inscripciones.css',
  templateUrl: './mis-inscripciones.html',
})
export class MisInscripciones {

  private readonly api = inject(InscripcionesComisionApi);

  protected readonly misInscripciones = rxResource({
    stream: () => this.api.listarInscripciones(),
  })

  protected readonly aBajar = signal<InscripcionComisionResponse | null>(null)
  protected readonly bajando = signal(false)
  protected readonly avisoBaja = signal<{ inscripcionId: number; texto: string; esError: boolean } | null>(null)

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
      this.avisoBaja.set({ inscripcionId: inscripcion.id, texto: 'Te diste de baja de ' + inscripcion.nombreMateria, esError: false });
    }catch(error)
    {
      this.avisoBaja.set({ inscripcionId: inscripcion.id, texto: mensajeDeError(error),esError: true})
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
