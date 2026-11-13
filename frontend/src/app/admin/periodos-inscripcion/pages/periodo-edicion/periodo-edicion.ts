import { afterNextRender, Component, ElementRef, inject, input, linkedSignal, signal, viewChild } from '@angular/core';
import { PeriodoInscripcionApi } from '../../data/periodos-inscripcion-api';
import { ActivatedRoute, Router } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ConfirmarDialog } from '../../../../shared/ui/confirmar-dialog/confirmar-dialog';
import { Icono } from '../../../../shared/ui/icono/icono';

@Component({
  imports: [FormRoot, FormField, ConfirmarDialog, Icono],
  selector: 'app-periodo-edicion',
  styleUrl: './periodo-edicion.css',
  templateUrl: './periodo-edicion.html',
})
export class PeriodoEdicion {

  private readonly api = inject(PeriodoInscripcionApi);
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo')

  readonly periodoId = input.required<string>();
  protected readonly mensaje = signal<string | null>(null);
  protected readonly borrado = signal(false);
  protected readonly cargando = signal(false)
  protected readonly error = signal<string | null>(null);

  protected readonly periodo = rxResource({
    params: () => Number(this.periodoId()),
    stream: ({params}) => this.api.obtenerPeriodo(params)
  });

  private readonly datos = linkedSignal(() => {
    const periodo = this.periodo.value();
    return{
      fechaInicio: periodo?.fechaInicio ?? '',
      fechaCierre: periodo?.fechaCierre ?? '',
    };
  });

   private async guardar(): Promise<void>
  {
    this.error.set(null);
    this.cargando.set(true);
    try {
      await firstValueFrom(this.api.modificarPeriodo(Number(this.periodoId()), this.datos()));
      this.mensaje.set('Se guardaron los cambios');
    } catch (error) {
      this.error.set(mensajeDeError(error));
    }
    this.cargando.set(false);
  }


  protected readonly periodoForm = form(
    this.datos,
    (campo) => {
      required(campo.fechaInicio, { message: 'Elegí la fecha de inicio' });
      required(campo.fechaCierre, { message: 'Elegí la fecha de cierre' });
    },
    {
      submission: {
        action: async () => {
          await this.guardar();
          return undefined;
        },
      },
    },
  );

  constructor()
  {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }


  protected async habilitar(): Promise<void>
  {
    this.error.set(null)
    this.cargando.set(true)
    try{
      await firstValueFrom(this.api.habilitar(Number(this.periodoId())))
      this.mensaje.set('Periodo habilitado');
    }catch (error)
    {
      this.error.set(mensajeDeError(error))
    }
    this.cargando.set(false);
  }

  protected async deshabilitar(): Promise<void>
  {
    this.error.set(null)
    this.cargando.set(true)
    try{
      await firstValueFrom(this.api.deshabilitar(Number(this.periodoId())))
      this.mensaje.set('Periodo deshabilitado');
    }catch (error)
    {
      this.error.set(mensajeDeError(error))
    }
    this.cargando.set(false);
  }


  protected async borrar(): Promise<void>
  {
    this.error.set(null)
    this.cargando.set(true)
    try{
       await firstValueFrom(this.api.borrarPeriodo(Number(this.periodoId())));
       this.mensaje.set('periodo borrado');
    }catch(error)
    {
      if(error instanceof HttpErrorResponse && error.status === 409)
      {
        this.error.set('El periodo tiene inscripciones o mesas y no se pude borrar, se pude deshabilitar');
      }else
      {
        this.error.set(mensajeDeError(error));
      }
    }
    this.borrado.set(false)
    this.cargando.set(false)
  }
  protected volver(): void
  {
    void this.router.navigate(['..'], {relativeTo: this.ruta})
  }

  protected cerrar(): void
  {
    this.dialogo().nativeElement.close();
  }

}


function mensajeDeError(error: unknown): string
{
  if (error instanceof HttpErrorResponse && error.status !== 0 && error.status !== 500) {
    return error.error?.errores?.[0]?.mensaje ?? error.error?.detail ?? 'Ocurrió un error';
  }
  return 'Ocurrio un error';
}
