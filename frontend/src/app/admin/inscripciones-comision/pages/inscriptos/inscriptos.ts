import { afterNextRender, Component, computed, ElementRef, inject, input, numberAttribute, signal, viewChild } from '@angular/core';
import { InscripcionesComisionApi } from '../../data/inscripcion-comision-api';
import { ComisionesAPi } from '../../../comisiones/data/comisiones-api';
import { ActivatedRoute, Router } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { InscriptoComisionResponse } from '../../data/inscripcion-comision';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';

@Component({
  imports: [DatePipe,ConfirmarDialog, Icono],
  selector: 'app-inscriptos',
  styleUrl: './inscriptos.css',
  templateUrl: './inscriptos.html',
})
export class Inscriptos {

  private readonly api = inject(InscripcionesComisionApi);
  private readonly comisionesApi = inject(ComisionesAPi);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  readonly comisionId = input.required({ transform: numberAttribute });
  readonly comisionMateriaId = input.required({ transform: numberAttribute });

  private readonly comision = rxResource({
    params: () => this.comisionId(),
    stream: ({ params }) => this.comisionesApi.obtenerComision(params),
  });


  protected readonly nombreMateria = computed(() =>
    this.comision.value()?.materias.find((m) => m.id === this.comisionMateriaId())?.nombreMateria ?? '',
  );

  protected readonly inscriptos = rxResource({
    params: () => this.comisionMateriaId(),
    stream: ({ params }) => this.api.listar(params),
  });

  protected readonly aBajar = signal<InscriptoComisionResponse | null>(null);
  protected readonly bajando = signal(false);
  protected readonly aviso = signal<{ texto: string; esError: boolean } | null>(null);

  constructor()
  {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  protected mensajeBaja(inscripto: InscriptoComisionResponse): string
  {
    return `Se va a dar de baja a ${inscripto.apellido}, ${inscripto.nombre} de ${this.nombreMateria()}.`;
  }

  protected async darDeBaja(inscripto: InscriptoComisionResponse): Promise<void>
  {
    this.aviso.set(null);
    this.bajando.set(true);
    try {
      await firstValueFrom(this.api.darDeBaja(inscripto.inscripcionId));
      this.aviso.set({ texto: `Se dio de baja a ${inscripto.apellido}, ${inscripto.nombre}`, esError: false });
      this.inscriptos.reload();
    } catch (error) {
      this.aviso.set({ texto: mensajeDeError(error), esError: true });
    }
    this.aBajar.set(null);
    this.bajando.set(false);
  }

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

  protected volver(): void
  {
    void this.router.navigate(['../../..'], { relativeTo: this.ruta });
  }
}

function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrió un error';
}
